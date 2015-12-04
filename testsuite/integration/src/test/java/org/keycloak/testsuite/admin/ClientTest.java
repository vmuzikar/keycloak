package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void getClients() {
        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker", Constants.ADMIN_CLI_CLIENT_ID);
    }

    private ClientRepresentation createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);
        rep.setId(id);
        return rep;
    }

    @Test
    public void createClientVerify() {
        String id = createClient().getId();

        assertNotNull(realm.clients().get(id));
        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker", "my-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void removeClient() {
        String id = createClient().getId();

        realm.clients().get(id).remove();
    }

    @Test
    public void getClientRepresentation() {
        String id = createClient().getId();

        ClientRepresentation rep = realm.clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    /**
     * See <a href="https://issues.jboss.org/browse/KEYCLOAK-1918">KEYCLOAK-1918</a>
     */
    @Test
    public void getClientDescription() {
        String id = createClient().getId();

        ClientRepresentation rep = realm.clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app description", rep.getDescription());
    }

    @Test
    public void getClientSessions() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        OAuthClient.AuthorizationCodeResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ClientResource app = ApiUtil.findClientByClientId(keycloak.realm("test"), "test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getClients().size());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setEnabled(true);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);

        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        realm.clients().get(id).roles().create(role);

        rep = realm.clients().get(id).toRepresentation();
        rep.setDefaultRoles(new String[] { "test" });
        realm.clients().get(id).update(rep);

        assertArrayEquals(new String[] { "test" }, realm.clients().get(id).toRepresentation().getDefaultRoles());

        realm.clients().get(id).roles().deleteRole("test");

        assertNull(realm.clients().get(id).toRepresentation().getDefaultRoles());
    }

    @Test
    public void testProtocolMappers() {
        createClient();
        ProtocolMappersResource mappersResource = ApiUtil.findClientByClientId(realm, "my-app").getProtocolMappers();

        protocolMappersTest(mappersResource);
    }

    @Test
    public void updateClient() {
        ClientRepresentation client = createClient();

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setId(client.getId());
        newClient.setClientId(client.getClientId());
        newClient.setBaseUrl("http://baseurl");

        realm.clients().get(client.getId()).update(newClient);

        ClientRepresentation storedClient = realm.clients().get(client.getId()).toRepresentation();

        assertClient(client, storedClient);

        newClient.setSecret("new-secret");

        realm.clients().get(client.getId()).update(newClient);

        storedClient = realm.clients().get(client.getId()).toRepresentation();
        assertClient(client, storedClient);
    }

    public static void protocolMappersTest(ProtocolMappersResource mappersResource) {
        // assert default mappers found
        List<ProtocolMapperRepresentation> protocolMappers = mappersResource.getMappers();

        String emailMapperId = null;
        String usernameMapperId = null;
        String fooMapperId = null;
        for (ProtocolMapperRepresentation mapper : protocolMappers) {
            if (mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)) {
                emailMapperId = mapper.getId();
            } else if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)) {
                usernameMapperId = mapper.getId();
            } else if (mapper.getName().equals("foo")) {
                fooMapperId = mapper.getId();
            }
        }

        assertNotNull(emailMapperId);
        assertNotNull(usernameMapperId);
        assertNull(fooMapperId);

        // Create foo mapper
        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("fooProtocol");
        fooMapper.setProtocolMapper("fooMapper");
        fooMapper.setConsentRequired(true);
        Response response = mappersResource.createMapper(fooMapper);
        String location = response.getLocation().toString();
        fooMapperId = location.substring(location.lastIndexOf("/") + 1);
        response.close();

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getName(), "foo");

        // Update foo mapper
        fooMapper.setProtocolMapper("foo-mapper-updated");
        mappersResource.update(fooMapperId, fooMapper);

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getProtocolMapper(), "foo-mapper-updated");

        // Remove foo mapper
        mappersResource.delete(fooMapperId);
        try {
            mappersResource.getMapperById(fooMapperId);
            fail("Not expected to find deleted mapper");
        } catch (NotFoundException nfe) {
        }
    }

    public static void assertClient(ClientRepresentation client, ClientRepresentation storedClient) {
        if (client.getClientId() != null) Assert.assertEquals(client.getClientId(), storedClient.getClientId());
        if (client.getName() != null) Assert.assertEquals(client.getName(), storedClient.getName());
        if (client.isEnabled() != null) Assert.assertEquals(client.isEnabled(), storedClient.isEnabled());
        if (client.isBearerOnly() != null) Assert.assertEquals(client.isBearerOnly(), storedClient.isBearerOnly());
        if (client.isPublicClient() != null) Assert.assertEquals(client.isPublicClient(), storedClient.isPublicClient());
        if (client.isFullScopeAllowed() != null) Assert.assertEquals(client.isFullScopeAllowed(), storedClient.isFullScopeAllowed());
        if (client.getRootUrl() != null) Assert.assertEquals(client.getRootUrl(), storedClient.getRootUrl());
        if (client.getAdminUrl() != null) Assert.assertEquals(client.getAdminUrl(), storedClient.getAdminUrl());
        if (client.getBaseUrl() != null) Assert.assertEquals(client.getBaseUrl(), storedClient.getBaseUrl());
        if (client.isSurrogateAuthRequired() != null) Assert.assertEquals(client.isSurrogateAuthRequired(), storedClient.isSurrogateAuthRequired());
        if (client.getClientAuthenticatorType() != null) Assert.assertEquals(client.getClientAuthenticatorType(), storedClient.getClientAuthenticatorType());

        if (client.getNotBefore() != null) {
            Assert.assertEquals(client.getNotBefore(), storedClient.getNotBefore());
        }
        if (client.getDefaultRoles() != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getDefaultRoles()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getDefaultRoles()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> redirectUris = client.getRedirectUris();
        if (redirectUris != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getRedirectUris()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getRedirectUris()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> webOrigins = client.getWebOrigins();
        if (webOrigins != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getWebOrigins()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getWebOrigins()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }
    }

}
