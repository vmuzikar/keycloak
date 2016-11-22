/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oauth;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createAddressMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createClaimMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedRole;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCProtocolMappersTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    @Test
    public void testTokenMapping() throws Exception {
        {
            UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            UserRepresentation user = userResource.toRepresentation();

            user.singleAttribute("street", "5 Yawkey Way");
            user.singleAttribute("locality", "Boston");
            user.singleAttribute("region", "MA");
            user.singleAttribute("postal_code", "02115");
            user.singleAttribute("country", "USA");
            user.singleAttribute("phone", "617-777-6666");

            List<String> departments = Arrays.asList("finance", "development");
            user.getAttributes().put("departments", departments);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            ProtocolMapperRepresentation mapper = createAddressMapper(true, true);
            app.getProtocolMappers().createMapper(mapper);

            ProtocolMapperRepresentation hard = createHardcodedClaim("hard", "hard", "coded", "String", false, null, true, true);
            app.getProtocolMappers().createMapper(hard);
            app.getProtocolMappers().createMapper(createHardcodedClaim("hard-nested", "nested.hard", "coded-nested", "String", false, null, true, true));
            app.getProtocolMappers().createMapper(createClaimMapper("custom phone", "phone", "home_phone", "String", true, "", true, true, false));
            app.getProtocolMappers().createMapper(createClaimMapper("nested phone", "phone", "home.phone", "String", true, "", true, true, false));
            app.getProtocolMappers().createMapper(createClaimMapper("departments", "departments", "department", "String", true, "", true, true, true));
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-realm", "hardcoded"));
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-app", "app.hardcoded"));
            app.getProtocolMappers().createMapper(createRoleNameMapper("rename-app-role", "test-app.customer-user", "realm-user"));
        }

        {
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getAddress());
            assertEquals(idToken.getName(), "Tom Brady");
            assertEquals(idToken.getAddress().getStreetAddress(), "5 Yawkey Way");
            assertEquals(idToken.getAddress().getLocality(), "Boston");
            assertEquals(idToken.getAddress().getRegion(), "MA");
            assertEquals(idToken.getAddress().getPostalCode(), "02115");
            assertEquals(idToken.getAddress().getCountry(), "USA");
            assertNotNull(idToken.getOtherClaims().get("home_phone"));
            assertEquals("617-777-6666", idToken.getOtherClaims().get("home_phone"));
            assertEquals("coded", idToken.getOtherClaims().get("hard"));
            Map nested = (Map) idToken.getOtherClaims().get("nested");
            assertEquals("coded-nested", nested.get("hard"));
            nested = (Map) idToken.getOtherClaims().get("home");
            assertEquals("617-777-6666", nested.get("phone"));
            List<String> departments = (List<String>) idToken.getOtherClaims().get("department");
            assertEquals(2, departments.size());
            assertTrue(departments.contains("finance") && departments.contains("development"));

            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals(accessToken.getName(), "Tom Brady");
            assertNotNull(accessToken.getAddress());
            assertEquals(accessToken.getAddress().getStreetAddress(), "5 Yawkey Way");
            assertEquals(accessToken.getAddress().getLocality(), "Boston");
            assertEquals(accessToken.getAddress().getRegion(), "MA");
            assertEquals(accessToken.getAddress().getPostalCode(), "02115");
            assertEquals(accessToken.getAddress().getCountry(), "USA");
            assertNotNull(accessToken.getOtherClaims().get("home_phone"));
            assertEquals("617-777-6666", accessToken.getOtherClaims().get("home_phone"));
            assertEquals("coded", accessToken.getOtherClaims().get("hard"));
            nested = (Map) accessToken.getOtherClaims().get("nested");
            assertEquals("coded-nested", nested.get("hard"));
            nested = (Map) accessToken.getOtherClaims().get("home");
            assertEquals("617-777-6666", nested.get("phone"));
            departments = (List<String>) idToken.getOtherClaims().get("department");
            assertEquals(2, departments.size());
            assertTrue(departments.contains("finance") && departments.contains("development"));
            assertTrue(accessToken.getRealmAccess().getRoles().contains("hardcoded"));
            assertTrue(accessToken.getRealmAccess().getRoles().contains("realm-user"));
            Assert.assertFalse(accessToken.getResourceAccess("test-app").getRoles().contains("customer-user"));
            assertTrue(accessToken.getResourceAccess("app").getRoles().contains("hardcoded"));
        }

        // undo mappers
        {
            ClientResource app = findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRepresentation = app.toRepresentation();
            for (ProtocolMapperRepresentation model : clientRepresentation.getProtocolMappers()) {
                if (model.getName().equals("address")
                        || model.getName().equals("hard")
                        || model.getName().equals("hard-nested")
                        || model.getName().equals("custom phone")
                        || model.getName().equals("departments")
                        || model.getName().equals("nested phone")
                        || model.getName().equals("rename-app-role")
                        || model.getName().equals("hard-realm")
                        || model.getName().equals("hard-app")
                        ) {
                    app.getProtocolMappers().delete(model.getId());
                }
            }
        }

        events.clear();


        {
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNull(idToken.getAddress());
            assertNull(idToken.getOtherClaims().get("home_phone"));
            assertNull(idToken.getOtherClaims().get("hard"));
            assertNull(idToken.getOtherClaims().get("nested"));
            assertNull(idToken.getOtherClaims().get("department"));
        }


        events.clear();
    }


    @Test
    public void testUserRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper("test-app", null, "Client roles mapper", "roles-custom.test-app", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppMappings = (String) roleMappings.get("test-app");
        assertRoles(realmRoleMappings,
          "pref.user",                      // from direct assignment in user definition
          "pref.offline_access"             // from direct assignment in user definition
        );
        assertRoles(testAppMappings,
          "customer-user"                   // from direct assignment in user definition
        );
    }


    @Test
    public void testUserGroupRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        String clientId = "test-app";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, "ta.", "Client roles mapper", "roles-custom.test-app", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppMappings = (String) roleMappings.get(clientId);
        assertRoles(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium",     // from client role customer-admin-composite-role - realm role for test-app
          "pref.realm-composite-role",      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.sample-realm-role"          // from realm role realm-composite-role
        );
        assertRoles(testAppMappings,
          "ta.customer-user",                  // from direct assignment to /roleRichGroup/level2group
          "ta.customer-admin-composite-role",  // from direct assignment to /roleRichGroup/level2group
          "ta.customer-admin",                 // from client role customer-admin-composite-role - client role for test-app
          "ta.sample-client-role"              // from realm role realm-composite-role - client role for test-app
        );
    }

    @Test
    public void testUserGroupRoleToAttributeMappersNotScopedOtherApp() throws Exception {
        String clientId = "test-app-authz";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom." + clientId, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppAuthzMappings = (String) roleMappings.get(clientId);
        assertRoles(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium",     // from client role customer-admin-composite-role - realm role for test-app
          "pref.realm-composite-role",      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.sample-realm-role"          // from realm role realm-composite-role
        );
        assertRoles(testAppAuthzMappings);  // There is no client role defined for test-app-authz
    }

    @Test
    public void testUserGroupRoleToAttributeMappersScoped() throws Exception {
        String clientId = "test-app-scope";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom.test-app-scope", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertRoles(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user"                       // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
        );
        assertRoles(testAppScopeMappings,
          "test-app-allowed-by-scope"       // from direct assignment to roleRichUser, present as scope allows it
        );
    }

    @Test
    public void testUserGroupRoleToAttributeMappersScopedClientNotSet() throws Exception {
        String clientId = "test-app-scope";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(null, null, "Client roles mapper", "roles-custom.test-app-scope", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertRoles(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user"                       // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
        );
        assertRoles(testAppScopeMappings,
          "test-app-allowed-by-scope",      // from direct assignment to roleRichUser, present as scope allows it
          "customer-admin-composite-role"   // from direct assignment to /roleRichGroup/level2group, present as scope allows it
        );
    }

    private void assertRoles(String actualRoleString, String...expectedRoles) {
        String[] roles;
        Assert.assertThat(actualRoleString.matches("^\\[.*\\]$"), is(true));
        roles = actualRoleString.substring(1, actualRoleString.length() - 1).split(",\\s*");

        if (expectedRoles == null || expectedRoles.length == 0) {
            Assert.assertThat(roles, arrayContainingInAnyOrder(""));
        } else {
            Assert.assertThat(roles, arrayContainingInAnyOrder(expectedRoles));
        }
    }


    private ProtocolMapperRepresentation makeMapper(String name, String mapperType, Map<String, String> config) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setName(name);
        rep.setProtocolMapper(mapperType);
        rep.setConfig(config);
        rep.setConsentRequired(true);
        rep.setConsentText("Test Consent Text");
        return rep;
    }

}
