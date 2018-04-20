package org.keycloak.testsuite.docker;

import com.github.dockerjava.api.model.ContainerNetwork;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import sun.security.provider.X509Factory;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class DockerClientTest extends AbstractKeycloakTest {
    public static final String REALM_ID = "docker-test-realm";
    public static final String CLIENT_ID = "docker-test-client";
    public static final String DOCKER_USER = "docker-user";
    public static final String DOCKER_USER_PASSWORD = "password";

    public static final String REGISTRY_HOSTNAME = "registry.localdomain";
    public static final Integer REGISTRY_PORT = 5000;
    public static final String MINIMUM_DOCKER_VERSION = "1.8.0";

    private GenericContainer dockerRegistryContainer = null;
    private GenericContainer dockerClientContainer = null;

    private static String hostIp;
    private static String authServerPort;

    @BeforeClass
    public static void verifyEnvironment() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.DOCKER);

        final Optional<DockerVersion> dockerVersion = new DockerHostVersionSupplier().get();
        assumeTrue("Could not determine docker version for host machine.  It either is not present or accessible to the JVM running the test harness.", dockerVersion.isPresent());
        assumeTrue("Docker client on host machine is not a supported version.  Please upgrade and try again.", DockerVersion.COMPARATOR.compare(dockerVersion.get(), DockerVersion.parseVersionString(MINIMUM_DOCKER_VERSION)) >= 0);

        hostIp = System.getProperty("host.ip");

        if (hostIp == null) {
            final Optional<String> foundHostIp = new DockerHostIpSupplier().get();
            if (foundHostIp.isPresent()) {
                hostIp = foundHostIp.get();
            }
        }
        Assert.assertNotNull("Could not resolve host machine's IP address for docker adapter, and 'host.ip' system poperty not set. Client will not be able to authenticate against the keycloak server!", hostIp);

        authServerPort = AUTH_SERVER_SSL_REQUIRED ? System.getProperty("auth.server.https.port") : System.getProperty("auth.server.http.port");
    }

    @Override
    public void addTestRealms(final List<RealmRepresentation> testRealms) {
        final RealmRepresentation dockerRealm = DockerTestRealmSetup.createRealm(REALM_ID);
        DockerTestRealmSetup.configureDockerRegistryClient(dockerRealm, CLIENT_ID);
        DockerTestRealmSetup.configureUser(dockerRealm, DOCKER_USER, DOCKER_USER_PASSWORD);

        testRealms.add(dockerRealm);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        // find the realm cert
        String realmCert = null;
        List<KeysMetadataRepresentation.KeyMetadataRepresentation> realmKeys = adminClient.realm(REALM_ID).keys().getKeyMetadata().getKeys();
        for (KeysMetadataRepresentation.KeyMetadataRepresentation key : realmKeys) {
            if (key.getType().equals("RSA")) {
                realmCert = key.getCertificate();
            }
        }
        if (realmCert == null) {
            throw new IllegalStateException("Cannot find public realm cert");
        }

        // save the cert to a file
        File tmpCertFile = File.createTempFile("keycloak-docker-realm-cert-", ".pem");
        tmpCertFile.deleteOnExit();
        PrintWriter tmpCertWriter = new PrintWriter(tmpCertFile);
        tmpCertWriter.println(X509Factory.BEGIN_CERT);
        tmpCertWriter.println(realmCert);
        tmpCertWriter.println(X509Factory.END_CERT);
        tmpCertWriter.close();

        final Map<String, String> environment = new HashMap<>();
        environment.put("REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY", "/tmp");
        environment.put("REGISTRY_AUTH_TOKEN_REALM", "http://" + hostIp + ":" + authServerPort + "/auth/realms/" + REALM_ID + "/protocol/docker-v2/auth");
        environment.put("REGISTRY_AUTH_TOKEN_SERVICE", CLIENT_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ISSUER", "http://" + hostIp + ":" + authServerPort + "/auth/realms/" + REALM_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ROOTCERTBUNDLE", "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName());
        environment.put("INSECURE_REGISTRY", "--insecure-registry " + REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);

        String dockerioPrefix = Boolean.parseBoolean(System.getProperty("docker.io-prefix-explicit")) ? "docker.io/" : "";

        dockerRegistryContainer = new GenericContainer(dockerioPrefix + "registry:2")
                .withFileSystemBind(tmpCertFile.getCanonicalPath(), "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName(), BindMode.READ_ONLY)
                .withEnv(environment)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerRegistryContainer")))
                .withPrivilegedMode(true);
        dockerRegistryContainer.start();

        dockerClientContainer = new GenericContainer(dockerioPrefix + "docker:stable-dind")
                .withClasspathResourceMapping("dockerClientTest/keycloak-docker-compose-yaml/daemon.json", "/etc/docker/daemon.json", BindMode.READ_WRITE)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerClientContainer")))
                .withPrivilegedMode(true);

        final Optional<ContainerNetwork> network = dockerRegistryContainer.getContainerInfo().getNetworkSettings().getNetworks().values().stream().findFirst();
        assertTrue("Could not find a network adapter whereby the docker client container could connect to host!", network.isPresent());
        dockerClientContainer.withExtraHost(REGISTRY_HOSTNAME, network.get().getIpAddress());

        dockerClientContainer.start();

        log.info("Waiting for docker service...");
        validateDockerStarted();
        log.info("Docker service successfully started");
    }

    @Override
    public void afterAbstractKeycloakTest() {
        super.afterAbstractKeycloakTest();

        dockerClientContainer.close();
        dockerRegistryContainer.close();
    }

    private void validateDockerStarted() {
        final Callable<Boolean> checkStrategy = () -> {
            try {
                final String commandResult = dockerClientContainer.execInContainer("docker ps").getStderr();
                return !commandResult.contains("Cannot connect");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                return false;
            }
        };

        Unreliables.retryUntilTrue(30, TimeUnit.SECONDS, () -> RateLimiterBuilder.newBuilder().withRate(1, TimeUnit.SECONDS).withConstantThroughput().build().getWhenReady(() -> {
            try {
                return checkStrategy.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Test
    public void shouldPerformDockerAuthAgainstRegistry() throws Exception {
        log.info("Starting the attempt for login...");
        Container.ExecResult dockerLoginResult = dockerClientContainer.execInContainer("docker", "login", "-u", DOCKER_USER, "-p", DOCKER_USER_PASSWORD, REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);
        log.infof("Command executed. Output follows:\nSTDOUT: %s\n---\nSTDERR: %s", dockerLoginResult.getStdout(), dockerLoginResult.getStderr());
        assertThat(dockerLoginResult.getStdout(), containsString("Login Succeeded"));
    }
}
