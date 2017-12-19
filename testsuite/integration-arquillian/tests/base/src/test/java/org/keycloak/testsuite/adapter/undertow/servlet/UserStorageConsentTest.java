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
package org.keycloak.testsuite.adapter.undertow.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.adapter.servlet.ProductServlet;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.pages.ConsentPage;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AppServerContainer("auth-server-undertow")
public class UserStorageConsentTest extends AbstractServletsAdapterTest {

    @Page
    private ProductPortal productPortal;

    @Page
    protected ConsentPage consentPage;



    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));

        addComponent(memProvider);
    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = testRealmResource().components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }




    public static void setupConsent(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("demo");
        ClientModel product = session.realms().getClientByClientId("product-portal", realm);
        product.setConsentRequired(true);
        ClientTemplateModel clientTemplate = realm.addClientTemplate("template");
        clientTemplate.setFullScopeAllowed(true);
        System.err.println("client template protocol mappers size: " + clientTemplate.getProtocolMappers().size());

        for (ProtocolMapperModel mapper : product.getProtocolMappers()) {
            if (mapper.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
                if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)
                        || mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)
                        || mapper.getName().equals(OIDCLoginProtocolFactory.GIVEN_NAME)
                        ) {
                    ProtocolMapperModel copy = new ProtocolMapperModel();
                    copy.setName(mapper.getName());
                    copy.setProtocol(mapper.getProtocol());
                    Map<String, String> config = new HashMap<>();
                    config.putAll(mapper.getConfig());
                    copy.setConfig(config);
                    copy.setProtocolMapper(mapper.getProtocolMapper());
                    copy.setConsentText(mapper.getConsentText());
                    clientTemplate.addProtocolMapper(copy);
                }
            }
            product.removeProtocolMapper(mapper);
        }
        product.setClientTemplate(clientTemplate);
        product.setUseTemplateMappers(true);
        product.setUseTemplateScope(true);
        product.setUseTemplateConfig(false);
    }

    /**
     * KEYCLOAK-5273
     *
     * @throws Exception
     */
    @Test
    public void testLogin() throws Exception {
        testingClient.server().run(UserStorageConsentTest::setupConsent);
        UserRepresentation memuser = new UserRepresentation();
        memuser.setUsername("memuser");
        String uid = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealmResource(), memuser, "password");
        System.out.println("uid: " + uid);
        Assert.assertTrue(uid.startsWith("f:"));  // make sure its federated
        RoleRepresentation roleRep = adminClient.realm("demo").roles().get("user").toRepresentation();
        List<RoleRepresentation> roleList = new ArrayList<>();
        roleList.add(roleRep);
        adminClient.realm("demo").users().get(uid).roles().realmLevel().add(roleList);



        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("memuser", "password");
        org.keycloak.testsuite.Assert.assertTrue(consentPage.isCurrent());
        consentPage.confirm();
        assertCurrentUrlEquals(productPortal.toString());
        Assert.assertTrue(driver.getPageSource().contains("iPhone"));
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, productPortal.toString())
                .build("demo").toString();

        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("memuser", "password");
        assertCurrentUrlEquals(productPortal.toString());
        Assert.assertTrue(driver.getPageSource().contains("iPhone"));   }



}
