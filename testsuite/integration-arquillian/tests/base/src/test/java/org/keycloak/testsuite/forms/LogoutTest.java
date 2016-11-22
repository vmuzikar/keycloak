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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.auth.page.account.AccountManagement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LogoutTest extends TestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountManagement accountManagementPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void logoutRedirect() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        String redirectUri = AppPage.baseUrl + "?logout";

        String logoutUrl = oauth.getLogoutUrl(redirectUri, null);
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, redirectUri).assertEvent();

        assertEquals(redirectUri, driver.getCurrentUrl());

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);
    }

    @Test
    public void logoutSession() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        String logoutUrl = oauth.getLogoutUrl(null, sessionId);
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).removeDetail(Details.REDIRECT_URI).assertEvent();

        assertEquals(logoutUrl, driver.getCurrentUrl());

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);
    }

    @Test
    public void logoutMultipleSessions() throws IOException {
        // Login session 1
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        // Check session 1 logged-in
        oauth.openLoginForm();
        events.expectLogin().session(sessionId).removeDetail(Details.USERNAME).assertEvent();

         //  Logout session 1 by redirect
        driver.navigate().to(oauth.getLogoutUrl(AppPage.baseUrl, null));
        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, AppPage.baseUrl).assertEvent();

         // Check session 1 not logged-in
        oauth.openLoginForm();
        assertEquals(oauth.getLoginFormUrl(), driver.getCurrentUrl());

        // Login session 3
        oauth.doLogin("test-user@localhost", "password");
        String sessionId3 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId3);

        // Check session 3 logged-in
        oauth.openLoginForm();
        events.expectLogin().session(sessionId3).removeDetail(Details.USERNAME).assertEvent();
    }

    //KEYCLOAK-2741
    @Test
    public void logoutWithRememberMe() {
        setRememberMe(true);
        
        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("test-user@localhost", "password");

            String sessionId = events.expectLogin().assertEvent().getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            assertEquals("test-user@localhost", loginPage.getUsername());

            loginPage.login("test-user@localhost", "password");
            
            //log out
            appPage.openAccount();
            accountManagementPage.signOut();
            // Assert rememberMe not checked nor username/email prefilled
            assertTrue(loginPage.isCurrent());
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("test-user@localhost", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }
    
    private void setRememberMe(boolean enabled) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        rep.setRememberMe(enabled);
        adminClient.realm("test").update(rep);
    }
}
