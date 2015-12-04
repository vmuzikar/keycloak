/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.actions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.MailUtil;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionEmailVerificationTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected VerifyEmailPage verifyEmailPage;

    @WebResource
    protected RegisterPage registerPage;

    @WebResource
    protected InfoPage infoPage;

    @Before
    public void before() {
        oauth.state("mystate"); // have to set this as keycloak validates that state is sent
        keycloakRule.configure(new KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                appRealm.setVerifyEmail(true);

                UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                user.setEmailVerified(false);
            }

        });
    }

    @Test
    public void verifyEmailExisting() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];
        
        String verificationUrl = getPasswordResetEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        Event sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        Assert.assertEquals(mailCodeId, verificationUrl.split("code=")[1].split("\\&")[0].split("\\.")[1]);

        driver.navigate().to(verificationUrl.trim());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).detail(Details.CODE_ID, mailCodeId).assertEvent();
    }

    @Test
    public void verifyEmailRegister() throws IOException, MessagingException {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "email@mail.com", "verifyEmail", "password", "password");

        String userId = events.expectRegister("verifyEmail", "email@mail.com").assertEvent().getUserId();

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        Event sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).user(userId).detail("username", "verifyemail").detail("email", "email@mail.com").assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        String verificationUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).user(userId).session(sessionId).detail("username", "verifyemail").detail("email", "email@mail.com").detail(Details.CODE_ID, mailCodeId).assertEvent();

        events.expectLogin().user(userId).session(sessionId).detail("username", "verifyemail").detail(Details.CODE_ID, mailCodeId).assertEvent();
    }

    @Test
    public void verifyEmailResend() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        Event sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost").assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        verifyEmailPage.clickResendEmail();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[1];

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").assertEvent(sendEvent);

        String verificationUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        events.expectLogin().session(sessionId).assertEvent();
    }

    @Test
    public void verifyEmailNewBrowserSession() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getPasswordResetEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        Event sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        Assert.assertEquals(mailCodeId, verificationUrl.split("code=")[1].split("\\&")[0].split("\\.")[1]);

        driver.manage().deleteAllCookies();

        driver.navigate().to(verificationUrl.trim());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        assertTrue(infoPage.isCurrent());
        assertEquals("Your email address has been verified.", infoPage.getInfo());

        loginPage.open();

        assertTrue(loginPage.isCurrent());
    }


    @Test
    public void verifyInvalidKeyOrCode() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());
        String resendEmailLink = verifyEmailPage.getResendEmailLink();
        String keyInsteadCodeURL = resendEmailLink.replace("code=", "key=");

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        Event sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        driver.navigate().to(keyInsteadCodeURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();

        String badKeyURL = KeycloakUriBuilder.fromUri(resendEmailLink).queryParam("key", "foo").build().toString();
        driver.navigate().to(badKeyURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .session(sessionId)
                .detail("email", "test-user@localhost")
                .detail(Details.CODE_ID, mailCodeId)
                .assertEvent();
    }
    
    public static String getPasswordResetEmailLink(MimeMessage message) throws IOException, MessagingException {
    	Multipart multipart = (Multipart) message.getContent();
    	
        final String textContentType = multipart.getBodyPart(0).getContentType();
        
        assertEquals("text/plain; charset=UTF-8", textContentType);
        
        final String textBody = (String) multipart.getBodyPart(0).getContent();
        final String textChangePwdUrl = MailUtil.getLink(textBody);
    	
        final String htmlContentType = multipart.getBodyPart(1).getContentType();
        
        assertEquals("text/html; charset=UTF-8", htmlContentType);
        
        final String htmlBody = (String) multipart.getBodyPart(1).getContent();
        final String htmlChangePwdUrl = MailUtil.getLink(htmlBody);
        
        assertEquals(htmlChangePwdUrl, textChangePwdUrl);

        return htmlChangePwdUrl;
    }

}
