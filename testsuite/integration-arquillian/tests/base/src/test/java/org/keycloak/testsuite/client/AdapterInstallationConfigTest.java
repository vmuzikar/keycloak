package org.keycloak.testsuite.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdapterInstallationConfigTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;
    private ClientRepresentation client2;
    private ClientRepresentation clientPublic;
    private String publicKey;

    @Before
    public void before() throws Exception {
        super.before();

        publicKey = adminClient.realm(REALM_NAME).toRepresentation().getPublicKey();

        client = new ClientRepresentation();
        client.setEnabled(true);
        client.setClientId("RegistrationAccessTokenTest");
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        client.setPublicClient(false);
        client.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client.setRootUrl("http://root");
        client = createClient(client);
        client.setSecret("RegistrationAccessTokenTestClientSecret");

        client2 = new ClientRepresentation();
        client2.setEnabled(true);
        client2.setClientId("RegistrationAccessTokenTest2");
        client2.setSecret("RegistrationAccessTokenTestClientSecret");
        client2.setPublicClient(false);
        client2.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client2.setRootUrl("http://root");
        client2 = createClient(client2);

        clientPublic = new ClientRepresentation();
        clientPublic.setEnabled(true);
        clientPublic.setClientId("RegistrationAccessTokenTestPublic");
        clientPublic.setPublicClient(true);
        clientPublic.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessTokenPublic");
        clientPublic.setRootUrl("http://root");
        clientPublic = createClient(clientPublic);
    }

    @Test
    public void getConfigWithRegistrationAccessToken() throws ClientRegistrationException {
        reg.auth(Auth.token(client.getRegistrationAccessToken()));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);
    }

    @Test
    public void getConfig() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), client.getSecret()));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);

        assertEquals(testContext.getAuthServerContextRoot() + "/auth", config.getAuthServerUrl());
        assertEquals("test", config.getRealm());

        assertEquals(1, config.getCredentials().size());
        assertEquals(client.getSecret(), config.getCredentials().get("secret"));

        assertEquals(publicKey, config.getRealmKey());
        assertEquals(client.getClientId(), config.getResource());
        assertEquals(SslRequired.EXTERNAL.name().toLowerCase(), config.getSslRequired());
    }

    @Test
    public void getConfigMissingSecret() throws ClientRegistrationException {
        reg.auth(null);

        try {
            reg.getAdapterConfig(client.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigWrongClient() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), client.getSecret()));

        try {
            reg.getAdapterConfig(client2.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigPublicClient() throws ClientRegistrationException {
        reg.auth(null);

        AdapterConfig config = reg.getAdapterConfig(clientPublic.getClientId());
        assertNotNull(config);

        assertEquals("test", config.getRealm());

        assertEquals(0, config.getCredentials().size());

        assertEquals(publicKey, config.getRealmKey());
        assertEquals(clientPublic.getClientId(), config.getResource());
        assertEquals(SslRequired.EXTERNAL.name().toLowerCase(), config.getSslRequired());
    }

}
