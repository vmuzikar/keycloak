package org.keycloak.testsuite.docker;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.WaitUtils;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

public class DockerClientTest extends AbstractKeycloakTest {
    public static final Logger LOGGER = LoggerFactory.getLogger(DockerClientTest.class);

    public static final String REALM_ID = "docker-test-realm";
    public static final String AUTH_FLOW = "docker-basic-auth-flow";
    public static final String CLIENT_ID = "docker-test-client";
    public static final String DOCKER_USER = "docker-user";
    public static final String DOCKER_USER_PASSWORD = "password";

    public static final String REGISTRY_HOSTNAME = "localhost";
    public static final Integer REGISTRY_PORT = 5000;
    public static final String MINIMUM_DOCKER_VERSION = "1.8.0";

    private GenericContainer dockerRegistryContainer = null;
    private GenericContainer dockerClientContainer = null;

    private static String hostIp;

    @BeforeClass
    public static void verifyEnvironment() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.DOCKER);

        final Optional<DockerVersion> dockerVersion = new DockerHostVersionSupplier().get();
        assumeTrue("Could not determine docker version for host machine.  It either is not present or accessible to the JVM running the test harness.", dockerVersion.isPresent());
        assumeTrue("Docker client on host machine is not a supported version.  Please upgrade and try again.", DockerVersion.COMPARATOR.compare(dockerVersion.get(), DockerVersion.parseVersionString(MINIMUM_DOCKER_VERSION)) >= 0);
        LOGGER.debug("Discovered valid docker client on host.  version: {}", dockerVersion);

        hostIp = System.getProperty("host.ip");

        if (hostIp == null) {
            final Optional<String> foundHostIp = new DockerHostIpSupplier().get();
            if (foundHostIp.isPresent()) {
                hostIp = foundHostIp.get();
            }
        }
        Assert.assertNotNull("Could not resolve host machine's IP address for docker adapter, and 'host.ip' system poperty not set. Client will not be able to authenticate against the keycloak server!", hostIp);
    }

    @Override
    public void addTestRealms(final List<RealmRepresentation> testRealms) {
        final RealmRepresentation dockerRealm = loadJson(getClass().getResourceAsStream("/docker-test-realm.json"), RealmRepresentation.class);

        /**
         * TODO fix test harness/importer NPEs when attempting to create realm from scratch.
         * Need to fix those, would be preferred to do this programmatically such that we don't have to keep realm elements
         * (I.E. certs, realm url) in sync with a flat file
         *
         * final RealmRepresentation dockerRealm = DockerTestRealmSetup.createRealm(REALM_ID);
         * DockerTestRealmSetup.configureDockerAuthenticationFlow(dockerRealm, AUTH_FLOW);
         */

        DockerTestRealmSetup.configureDockerRegistryClient(dockerRealm, CLIENT_ID);
        DockerTestRealmSetup.configureUser(dockerRealm, DOCKER_USER, DOCKER_USER_PASSWORD);

        testRealms.add(dockerRealm);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        final Map<String, String> environment = new HashMap<>();
        environment.put("REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY", "/tmp");
        environment.put("REGISTRY_HTTP_TLS_CERTIFICATE", "/opt/certs/localhost.crt");
        environment.put("REGISTRY_HTTP_TLS_KEY", "/opt/certs/localhost.key");
        environment.put("REGISTRY_AUTH_TOKEN_REALM", "http://" + hostIp + ":8180/auth/realms/docker-test-realm/protocol/docker-v2/auth");
        environment.put("REGISTRY_AUTH_TOKEN_SERVICE", CLIENT_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ISSUER", "http://" + hostIp + ":8180/auth/realms/docker-test-realm");
        environment.put("REGISTRY_AUTH_TOKEN_ROOTCERTBUNDLE", "/opt/certs/docker-realm-public-key.pem");
        environment.put("INSECURE_REGISTRY", "--insecure-registry " + REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);

        String dockerioPrefix = Boolean.parseBoolean(System.getProperty("docker.io-prefix-explicit")) ? "docker.io/" : "";

        dockerRegistryContainer = new GenericContainer(dockerioPrefix + "registry:2")
                .withClasspathResourceMapping("dockerClientTest/keycloak-docker-compose-yaml/certs", "/opt/certs", BindMode.READ_ONLY)
                .withEnv(environment)
                .withNetworkMode("host")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withPrivilegedMode(true);
        dockerRegistryContainer.start();

        dockerClientContainer = new GenericContainer(dockerioPrefix + "docker:dind")
                .withNetworkMode("host")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withPrivilegedMode(true);

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
        Container.ExecResult dockerLoginResult = dockerClientContainer.execInContainer("docker", "login", "-u", DOCKER_USER, "-p", DOCKER_USER_PASSWORD + "xab", REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);
        log.infof("Command executed. Output follows:\nSTDOUT: %s\n---\nSTDERR: %s", dockerLoginResult.getStdout(), dockerLoginResult.getStderr());
        assertThat(dockerLoginResult.getStdout(), containsString("Login Succeeded"));
    }
}
