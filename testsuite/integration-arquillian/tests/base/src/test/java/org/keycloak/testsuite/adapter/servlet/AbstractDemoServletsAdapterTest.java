package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.page.CustomerDb;
import org.keycloak.testsuite.adapter.page.CustomerDbErrorPage;
import org.keycloak.testsuite.adapter.page.CustomerPortal;
import org.keycloak.testsuite.adapter.page.InputPortal;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.adapter.page.SecurePortal;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import org.keycloak.util.BasicAuthHelper;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractDemoServletsAdapterTest extends AbstractServletsAdapterTest {

    @Page
    private CustomerPortal customerPortal;
    @Page
    private SecurePortal securePortal;
    @Page
    private CustomerDb customerDb;
    @Page
    private CustomerDbErrorPage customerDbErrorPage;
    @Page
    private ProductPortal productPortal;
    @Page
    private InputPortal inputPortal;

    @Deployment(name = CustomerPortal.DEPLOYMENT_NAME)
    protected static WebArchive customerPortal() {
        return servletDeployment(CustomerPortal.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class);
    }

    @Deployment(name = SecurePortal.DEPLOYMENT_NAME)
    protected static WebArchive securePortal() {
        return servletDeployment(SecurePortal.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return servletDeployment(CustomerDb.DEPLOYMENT_NAME, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CustomerDbErrorPage.DEPLOYMENT_NAME)
    protected static WebArchive customerDbErrorPage() {
        return servletDeployment(CustomerDbErrorPage.DEPLOYMENT_NAME, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = InputPortal.DEPLOYMENT_NAME)
    protected static WebArchive inputPortal() {
        return servletDeployment(InputPortal.DEPLOYMENT_NAME, "keycloak.json", InputServlet.class);
    }

    @Test
    public void testSavedPostRequest() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        inputPortal.navigateTo();
        assertCurrentUrlEquals(inputPortal);
        inputPortal.execute("hello");

        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertEquals(driver.getCurrentUrl(), inputPortal + "/secured/post");
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("parameter=hello"));

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString())
                .build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // test unsecured POST KEYCLOAK-901
        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(inputPortal + "/unsecured").request().post(Entity.form(form), String.class);
        assertTrue(text.contains("parameter=hello"));
        client.close();
    }

    @Test
    public void testLoginSSOAndLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // View stats
        List<Map<String, String>> stats = testRealmResource().getClientSessionStats();
        Map<String, String> customerPortalStats = null;
        Map<String, String> productPortalStats = null;
        for (Map<String, String> s : stats) {
            switch (s.get("clientId")) {
                case "customer-portal":
                    customerPortalStats = s;
                    break;
                case "product-portal":
                    productPortalStats = s;
                    break;
            }
        }
        assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
        assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
//        testRealmLoginPage.form().cancel();
//        assertTrue(driver.getPageSource().contains("Error Page"));
    }

    @Test
    public void testServletRequestLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        customerPortal.navigateTo();
        assertCurrentUrlEquals(customerPortal);
        pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to(customerPortal + "/logout");
        assertTrue(driver.getPageSource().contains("servlet logout ok"));

        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testLoginSSOIdle() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        demoRealmRep.setSsoSessionIdleTimeout(1);
        testRealmResource().update(demoRealmRep);

//		Thread.sleep(2000);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
        testRealmResource().update(demoRealmRep);
    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        log.info("Current url: " + driver.getCurrentUrl());
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        log.info("Current url: " + driver.getCurrentUrl());
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        log.info(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        demoRealmRep.setSsoSessionIdleTimeout(1);
        testRealmResource().update(demoRealmRep);

        Time.setOffset(2);

        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // need to cleanup so other tests don't fail, so invalidate http sessions on remote clients.
        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
        // note: sessions invalidated after each test, see: AbstractKeycloakTest.afterAbstractKeycloakTest()

        Time.setOffset(0);
    }

    @Test
    public void testLoginSSOMax() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionMaxLifespan();
        demoRealmRep.setSsoSessionMaxLifespan(1);
        testRealmResource().update(demoRealmRep);

        TimeUnit.SECONDS.sleep(2);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
        testRealmResource().update(demoRealmRep);
    }

    @Test
    public void testNullBearerToken() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDb.toString());
        Response response = target.request().get();
        assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        assertEquals(401, response.getStatus());
        response.close();
        client.close();
    }

    @Test
    public void testNullBearerTokenCustomErrorPage() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDbErrorPage.toString());
        Response response = target.request().get();

        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        String errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        client.close();
    }

    @Test
    public void testBadUser() {
        Client client = ClientBuilder.newClient();
        URI uri = OIDCLoginProtocolService.tokenUrl(authServerPage.createUriBuilder()).build("demo");
        WebTarget target = client.target(uri);
        String header = BasicAuthHelper.createHeader("customer-portal", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "monkey@redhat.com")
                .param("password", "password");
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        assertEquals(401, response.getStatus());
        response.close();
        client.close();

    }

    @Test
    public void testVersion() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(authServerPage.createUriBuilder()).path("version");
        VersionRepresentation version = target.request().get(VersionRepresentation.class);
        assertNotNull(version);
        assertNotNull(version.getVersion());
        assertNotNull(version.getBuildTime());
        assertNotEquals(version.getVersion(), Version.UNKNOWN);
        assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

        VersionRepresentation version2 = client.target(securePortal.toString()).path(AdapterConstants.K_VERSION).request().get(VersionRepresentation.class);
        assertNotNull(version2);
        assertNotNull(version2.getVersion());
        assertNotNull(version2.getBuildTime());
        assertEquals(version.getVersion(), version2.getVersion());
        assertEquals(version.getBuildTime(), version2.getBuildTime());
        client.close();
    }

    @Test
    public void testAuthenticated() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(securePortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

}
