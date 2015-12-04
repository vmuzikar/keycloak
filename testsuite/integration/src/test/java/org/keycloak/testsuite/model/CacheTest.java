package org.keycloak.testsuite.model;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.models.cache.infinispan.ClientAdapter;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.testsuite.rule.KeycloakRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CacheTest {
    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    @Test
    public void testStaleCache() throws Exception {
        String appId = null;
        {
            // load up cache
            KeycloakSession session = kc.startSession();
            RealmModel realm = session.realms().getRealmByName("test");
            assertTrue(realm instanceof RealmAdapter);
            ClientModel testApp = realm.getClientByClientId("test-app");
            assertTrue(testApp instanceof ClientAdapter);
            assertNotNull(testApp);
            appId = testApp.getId();
            assertTrue(testApp.isEnabled());
            kc.stopSession(session, true);
        }
        {
            // update realm, then get an AppModel and change it.  The AppModel would not be a cache adapter
            KeycloakSession session = kc.startSession();

            // KEYCLOAK-1240 - obtain the realm via session.realms().getRealms()
            RealmModel realm = null;
            List<RealmModel> realms = session.realms().getRealms();

            for (RealmModel current : realms) {
                assertTrue(current instanceof RealmAdapter);
                if ("test".equals(current.getName())) {
                    realm = current;
                    break;
                }
            }

            realm.setAccessCodeLifespanLogin(200);
            ClientModel testApp = realm.getClientByClientId("test-app");

            assertNotNull(testApp);
            testApp.setEnabled(false);
            kc.stopSession(session, true);
        }
        // make sure that app cache was flushed and enabled changed
        {
            KeycloakSession session = kc.startSession();
            RealmModel realm = session.realms().getRealmByName("test");
            Assert.assertEquals(200, realm.getAccessCodeLifespanLogin());
            ClientModel testApp = session.realms().getClientById(appId, realm);
            Assert.assertFalse(testApp.isEnabled());
            kc.stopSession(session, true);
        }
    }

    @Test
    public void testAddUserNotAddedToCache() {
        KeycloakSession session = kc.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");

            UserModel user = session.users().addUser(realm, "testAddUserNotAddedToCache");
            user.setFirstName("firstName");
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, "testAddUserNotAddedToCache", "127.0.0.1", "auth", false, null, null);
            UserModel user2 = userSession.getUser();

            user.setLastName("lastName");

            assertNotNull(user2.getLastName());
        } finally {
            session.getTransaction().commit();
            session.close();
        }
    }

    // KEYCLOAK-1842
    @Test
    public void testRoleMappingsInvalidatedWhenClientRemoved() {
        KeycloakSession session = kc.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().addUser(realm, "joel");
            ClientModel client = realm.addClient("foo");
            RoleModel fooRole = client.addRole("foo-role");
            user.grantRole(fooRole);
        } finally {
            session.getTransaction().commit();
            session.close();
        }

        // Remove client
        session = kc.startSession();
        int grantedRolesCount;
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("joel", realm);
            grantedRolesCount = user.getRoleMappings().size();

            ClientModel client = realm.getClientByClientId("foo");
            realm.removeClient(client.getId());
        } finally {
            session.getTransaction().commit();
            session.close();
        }

        // Assert role mappings was removed from user as well
        session = kc.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("joel", realm);
            Set<RoleModel> roles = user.getRoleMappings();
            for (RoleModel role : roles) {
                Assert.assertNotNull(role.getContainer());
            }

            Assert.assertEquals(roles.size(), grantedRolesCount - 1);
        } finally {
            session.getTransaction().commit();
            session.close();
        }
    }

}
