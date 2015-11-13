/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.idp;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.idp.AbstractIdentityProvider;
import org.keycloak.testsuite.auth.page.login.idp.Facebook;
import org.keycloak.testsuite.auth.page.login.idp.OIDC;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.ClientCredentials;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.clients.CreateClient;
import org.keycloak.testsuite.console.page.idp.IdentityProviders;
import org.keycloak.testsuite.console.page.idp.IdpForm;
import org.keycloak.testsuite.console.page.idp.OIDCForm;
import org.keycloak.testsuite.console.page.users.Users;
import java.util.Collections;
import java.util.List;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.admin.ApiUtil.*;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProviderTest extends AbstractConsoleTest {
    public static final String OIDC_REALM = "OIDC-provider";
    public static final String OIDC_PROVIDER_NAME = "OpenID Connect v1.0";
    public static final String OIDC_CLIENT_NAME = "oidc-test-client";
    public static final String FACEBOOK_PROVIDER_NAME = "Facebook";

    @Page
    private IdentityProviders identityProvidersPage;

    /**
     * Generic form for managing IDPs
     */
    @Page
    private IdpForm genericIdpForm;

    /**
     * Specific form for managing OIDC IDP
     */
    @Page
    private OIDCForm oidcIdpForm;

    /**
     * Page for loggin using OIDC
     */
    @Page
    private OIDC oidcIdpLogin;

    /**
     * Page for loggin using Facebook
     */
    @Page
    private Facebook facebookIdpLogin;

    @Page
    private Clients clientsPage;

    @Page
    private Client clientPage;

    @Page
    private CreateClient createClientPage;

    @Page
    private ClientCredentials clientCredentialsPage;

    @Page
    private Users usersPage;

    @Page
    private AuthRealm oidcRealmPage;

    @Page
    private OIDCLogin oidcProviderLoginPage;

    @Before
    public void beforeIdpTest() {
        accountPage.setAuthRealm(AuthRealm.TEST);
        identityProvidersPage.navigateTo();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation oidcRealm = new RealmRepresentation();
        oidcRealm.setRealm(OIDC_REALM);
        oidcRealm.setEnabled(true);
        testRealms.add(oidcRealm);
    }

    private void addGenericIdp(String providerName, String clientId, String clientSecret) {
        identityProvidersPage.table().addProvider(providerName);
        genericIdpForm.setClientId(clientId);
        genericIdpForm.setClientSecret(clientSecret);
        genericIdpForm.setFirstLoginFlow(IdpForm.FIRST_BROKER_LOGIN);
        genericIdpForm.save();
        assertFlashMessageSuccess();
    }

    private void tryToLoginWithIdp(AbstractIdentityProvider identityProvider, String newUserName) {
        logoutFromMasterRealmConsole();

        accountPage.navigateTo();
        testRealmLoginPage.form().loginWithIdp(identityProvider);
        assertCurrentUrlStartsWith(accountPage);

        deleteAllCookiesForMasterRealm();
        deleteAllCookiesForTestRealm();
        loginToMasterRealmAdminConsoleAs(adminUser);

        // Try to find the new user in admin console
        usersPage.navigateTo();
        UserRepresentation user = usersPage.table().findUser(newUserName);
        Assert.assertNotNull(user);
        Assert.assertEquals(identityProvider.getEmail(), user.getEmail());
        Assert.assertEquals(identityProvider.getFirstName(), user.getFirstName());
        Assert.assertEquals(identityProvider.getLastName(), user.getLastName());
    }

    //FIXME: "Element is no longer attached to the DOM" exception might appear
    @Test
    public void testOIDC() throws InterruptedException {
        // Set the new realm which will act as an OIDC Provider
        oidcRealmPage.setAuthRealm(OIDC_REALM);
        oidcProviderLoginPage.setAuthRealm(OIDC_REALM);
        RealmResource oidcProviderRealmResource = adminClient.realm(oidcRealmPage.getAuthRealm());

        // Create a new user in the new realm
        UserRepresentation oidcUser = createUserRepresentation("oidc-test", "oidc@email.test", "oidc", "test", true);
        setPasswordFor(oidcUser, PASSWORD);
        String userId = createUserAndResetPasswordWithAdminClient(oidcProviderRealmResource, oidcUser, PASSWORD);
        oidcUser.setId(userId);
        assignClientRoles(oidcProviderRealmResource, userId, "realm-management", "view-realm");

        identityProvidersPage.table().addProvider(OIDC_PROVIDER_NAME);
        String redirectUri = oidcIdpForm.getRedirectUrl();

        // Create client in the OIDC realm
        clientsPage.setConsoleRealm(OIDC_REALM);
        clientsPage.navigateTo();
        clientsPage.table().createClient();
        createClientPage.form().setClientId(OIDC_CLIENT_NAME);
        createClientPage.form().setRedirectUris(Collections.singletonList(redirectUri));
        createClientPage.form().save();
        assertFlashMessageSuccess();

        clientPage.tabs().credentials();
        String clientSecret = clientCredentialsPage.getSecret();

        // Create IDP in standard Test realm
        identityProvidersPage.navigateTo();
        identityProvidersPage.table().addProvider(OIDC_PROVIDER_NAME);
        oidcIdpForm.setAuthorizationUrl(oidcProviderLoginPage.getOIDCLoginUrl().toString());
        oidcIdpForm.setTokenUrl(oidcProviderLoginPage.getOIDCTokenUrl().toString());
        oidcIdpForm.setClientId(OIDC_CLIENT_NAME);
        oidcIdpForm.setClientSecret(clientSecret);
        oidcIdpForm.setFirstLoginFlow(IdpForm.FIRST_BROKER_LOGIN);
        oidcIdpForm.save();
        assertFlashMessageSuccess();

        // Login to standard test realm using OIDC realm
        oidcIdpLogin.setUser(oidcUser);
        tryToLoginWithIdp(oidcIdpLogin, oidcIdpLogin.getProvider() + "." + oidcUser.getUsername());
    }

    @Test
    public void testFacebook() {
        addGenericIdp(FACEBOOK_PROVIDER_NAME, Facebook.CLIENT_ID, Facebook.SECRET);
        tryToLoginWithIdp(facebookIdpLogin, "facebook." + Facebook.EMAIL);
    }
}
