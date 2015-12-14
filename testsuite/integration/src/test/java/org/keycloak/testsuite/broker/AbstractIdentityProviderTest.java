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
package org.keycloak.testsuite.broker;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.MailUtil;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet.UserSessionStatus;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.LoggingRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityProviderTest {

    protected static final URI BASE_URI = UriBuilder.fromUri("http://localhost:8081/auth").build();

    @ClassRule
    public static BrokerKeyCloakRule brokerServerRule = new BrokerKeyCloakRule();

    @Rule
    public LoggingRule loggingRule = new LoggingRule(this);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginUpdateProfilePage updateProfilePage;


    @WebResource
    protected VerifyEmailPage verifyEmailPage;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected OAuthGrantPage grantPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    @WebResource
    protected AccountFederatedIdentityPage accountFederatedIdentityPage;

    protected KeycloakSession session;

    @Before
    public void onBefore() {
        this.session = brokerServerRule.startSession();
        removeTestUsers();
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
        assertNotNull(getIdentityProviderModel());
    }

    @After
    public void onAfter() {
        revokeGrant();
        brokerServerRule.stopSession(this.session, true);
    }

    protected UserModel assertSuccessfulAuthentication(IdentityProviderModel identityProviderModel, String username, String expectedEmail, boolean isProfileUpdateExpected) {
        authenticateWithIdentityProvider(identityProviderModel, username, isProfileUpdateExpected);

        // authenticated and redirected to app
        assertTrue("Bad current URL " + this.driver.getCurrentUrl() + " and page source: " + this.driver.getPageSource(),
                this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
        assertNotNull(federatedUser.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - federatedUser.getCreatedTimestamp()) < 10000);

        doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail, isProfileUpdateExpected);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

        RealmModel realm = getRealm();

        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(federatedUser.getUsername(), federatedIdentityModel.getIdentityProvider() + "." + federatedIdentityModel.getUserName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        return federatedUser;
    }



    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("kc-oidc-idp.test-user-noemail", federatedUser.getUsername());
        assertEquals(null, federatedUser.getEmail());
        assertEquals("Test", federatedUser.getFirstName());
        assertEquals("User", federatedUser.getLastName());
    }

    protected void authenticateWithIdentityProvider(IdentityProviderModel identityProviderModel, String username, boolean isProfileUpdateExpected) {
        loginIDP(username);


        if (isProfileUpdateExpected) {
            String userEmail = "new@email.com";
            String userFirstName = "New first";
            String userLastName = "New last";

            // update profile
            this.updateProfilePage.assertCurrent();
            this.updateProfilePage.update(userFirstName, userLastName, userEmail);
        }

    }

    protected void loginIDP(String username) {
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        String currentUrl = this.driver.getCurrentUrl();
        assertTrue(currentUrl.startsWith("http://localhost:8082/auth/"));
        System.out.println(this.driver.getCurrentUrl());
        // log in to identity provider
        this.loginPage.login(username, "password");
        doAfterProviderAuthentication();
    }

    protected UserModel getFederatedUser() {
        UserSessionStatus userSessionStatus = retrieveSessionStatus();
        IDToken idToken = userSessionStatus.getIdToken();
        KeycloakSession samlServerSession = brokerServerRule.startSession();
        try {
            RealmModel brokerRealm = samlServerSession.realms().getRealm("realm-with-broker");
            return samlServerSession.users().getUserById(idToken.getSubject(), brokerRealm);
        } finally {
            brokerServerRule.stopSession(samlServerSession, false);
        }
    }

    protected void doAfterProviderAuthentication() {

    }

    protected void revokeGrant() {

    }

    protected abstract String getProviderId();


    protected IdentityProviderModel getIdentityProviderModel() {
        IdentityProviderModel identityProviderModel = getRealm().getIdentityProviderByAlias(getProviderId());

        assertNotNull(identityProviderModel);

        identityProviderModel.setEnabled(true);

        return identityProviderModel;
    }


    protected RealmModel getRealm() {
        return getRealm(this.session);
    }

    protected static RealmModel getRealm(KeycloakSession session) {
        return session.realms().getRealm("realm-with-broker");
    }


    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail, boolean isProfileUpdateExpected) {
        if (isProfileUpdateExpected) {
            String userFirstName = "New first";
            String userLastName = "New last";

            assertEquals(expectedEmail, federatedUser.getEmail());
            assertEquals(userFirstName, federatedUser.getFirstName());
            assertEquals(userLastName, federatedUser.getLastName());
        } else {
            assertEquals(expectedEmail, federatedUser.getEmail());
            assertEquals("Test", federatedUser.getFirstName());
            assertEquals("User", federatedUser.getLastName());
        }
    }


    private void removeTestUsers() {
        RealmModel realm = getRealm();
        List<UserModel> users = this.session.users().getUsers(realm, true);

        for (UserModel user : users) {
            Set<FederatedIdentityModel> identities = this.session.users().getFederatedIdentities(user, realm);

            for (FederatedIdentityModel fedIdentity : identities) {
                this.session.users().removeFederatedIdentity(realm, user, fedIdentity.getIdentityProvider());
            }

            if (!"pedroigor".equals(user.getUsername())) {
                this.session.users().removeUser(realm, user);
            }
        }
    }


    protected void setUpdateProfileFirstLogin(final String updateProfileFirstLogin) {
        KeycloakModelUtils.runJobInTransaction(this.session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realm = getRealm(session);
                setUpdateProfileFirstLogin(realm, updateProfileFirstLogin);
            }

        });
    }

    protected static void setUpdateProfileFirstLogin(RealmModel realm, String updateProfileFirstLogin) {
        AuthenticatorConfigModel reviewProfileConfig = realm.getAuthenticatorConfigByAlias(DefaultAuthenticationFlows.IDP_REVIEW_PROFILE_CONFIG_ALIAS);
        reviewProfileConfig.getConfig().put(IdpReviewProfileAuthenticatorFactory.UPDATE_PROFILE_ON_FIRST_LOGIN, updateProfileFirstLogin);
        realm.updateAuthenticatorConfig(reviewProfileConfig);
    }


    protected UserSessionStatusServlet.UserSessionStatus retrieveSessionStatus() {
        UserSessionStatusServlet.UserSessionStatus sessionStatus = null;

        try {
            String pageSource = this.driver.getPageSource();

            sessionStatus = JsonSerialization.readValue(pageSource.getBytes(), UserSessionStatusServlet.UserSessionStatus.class);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        return sessionStatus;
    }

    protected String getVerificationEmailLink(MimeMessage message) throws IOException, MessagingException {
        Multipart multipart = (Multipart) message.getContent();

        final String textContentType = multipart.getBodyPart(0).getContentType();

        assertEquals("text/plain; charset=UTF-8", textContentType);

        final String textBody = (String) multipart.getBodyPart(0).getContent();
        final String textVerificationUrl = MailUtil.getLink(textBody);

        final String htmlContentType = multipart.getBodyPart(1).getContentType();

        assertEquals("text/html; charset=UTF-8", htmlContentType);

        final String htmlBody = (String) multipart.getBodyPart(1).getContent();
        final String htmlVerificationUrl = MailUtil.getLink(htmlBody);

        assertEquals(htmlVerificationUrl, textVerificationUrl);

        return htmlVerificationUrl;
    }
}
