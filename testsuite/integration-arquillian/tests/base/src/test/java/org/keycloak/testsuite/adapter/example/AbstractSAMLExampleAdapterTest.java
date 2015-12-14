package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.SAMLPostEncExample;
import org.keycloak.testsuite.adapter.page.SAMLPostSigExample;
import org.keycloak.testsuite.adapter.page.SAMLRedirectSigExample;
import org.keycloak.testsuite.util.URLAssert;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLDEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author mhajas
 */
public abstract class AbstractSAMLExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private SAMLPostSigExample samlPostSigExamplePage;

    @Page
    private SAMLPostEncExample samlPostEncExamplePage;

    @Page
    private SAMLRedirectSigExample samlRedirectSigExamplePage;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation samlRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/saml/testsaml.json"));
        testRealms.add(samlRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(SAMLDEMO);
        testRealmSAMLLoginPage.setAuthRealm(SAMLDEMO);
    }

    @Deployment(name = SAMLPostSigExample.DEPLOYMENT_NAME)
    private static WebArchive samlPostSigExampleDeployment() throws IOException {
        return exampleDeployment(SAMLPostSigExample.DEPLOYMENT_NAME);
    }

    @Deployment(name = SAMLPostEncExample.DEPLOYMENT_NAME)
    private static WebArchive samlPostEncExampleDeployment() throws IOException {
        return exampleDeployment(SAMLPostEncExample.DEPLOYMENT_NAME);
    }

    @Deployment(name = SAMLRedirectSigExample.DEPLOYMENT_NAME)
    private static WebArchive samlRedirectSigExampleDeployment() throws IOException {
        return exampleDeployment(SAMLRedirectSigExample.DEPLOYMENT_NAME);
    }

    @Test
    public void samlPostWithSignatureExampleTest() {
        samlPostSigExamplePage.navigateTo();
        testRealmSAMLLoginPage.form().login(bburkeUser);

        assertTrue(driver.getPageSource().contains("Welcome to the Sales Tool, " + bburkeUser.getUsername()));

        samlPostSigExamplePage.logout();

        samlPostSigExamplePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
    }

    @Test
    public void samlPostWithEncryptionExampleTest() {
        samlPostEncExamplePage.navigateTo();

        testRealmSAMLLoginPage.form().login(bburkeUser);

        assertTrue(driver.getPageSource().contains("Welcome to the Sales Tool, " + bburkeUser.getUsername()));

        samlPostEncExamplePage.logout();

        samlPostEncExamplePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
    }

    @Test
    public void samlRedirectWithSignatureExampleTest() {
        samlRedirectSigExamplePage.navigateTo();

        testRealmSAMLLoginPage.form().login(bburkeUser);

        assertTrue(driver.getPageSource().contains("Welcome to the Employee Tool,"));

        samlRedirectSigExamplePage.logout();

        samlRedirectSigExamplePage.navigateTo();
        URLAssert.assertCurrentUrlStartsWith(testRealmSAMLLoginPage);
    }
}
