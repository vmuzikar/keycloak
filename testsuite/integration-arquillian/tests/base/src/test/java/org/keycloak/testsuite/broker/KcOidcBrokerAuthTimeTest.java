/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.models.Constants.KC_ACTION;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KcOidcBrokerAuthTimeTest extends AbstractInitializedBaseBrokerTest {
    private final static Long AUTH_TIME_VALUE = 123L;

    // "mock" the IdP's auth_time value by overriding the actual auth_time using a mapper
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clients = super.createProviderClients();
                List<ProtocolMapperRepresentation> mappers = new ArrayList<>();

                ProtocolMapperRepresentation hardcodedClaim = createHardcodedClaim("auth_time-override",
                        "auth_time", String.valueOf(AUTH_TIME_VALUE), "long", false, true);

                mappers.add(hardcodedClaim);
                clients.get(0).setProtocolMappers(mappers);

                return clients;
            }
        };
    }

    @Test
    public void testAuthTimeProvidedByBroker() throws Exception {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("broker-app");

        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth
                .doLoginSocial(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());
        String code = authzResponse.getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "broker-app-secret");
        IDToken idToken = new JWSInput(response.getIdToken()).readJsonContent(IDToken.class);

        assertEquals(AUTH_TIME_VALUE, idToken.getAuth_time());
    }

    @Test
    public void testClaimPresent() throws Exception {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        String claimsParamStr = getProviderLoginParam(OIDCLoginProtocol.CLAIMS_PARAM);

        ObjectNode claimsParam = JsonSerialization.readValue(claimsParamStr, ObjectNode.class);
        assertTrue(claimsParam.get("id_token").get("auth_time").get("essential").asBoolean());
    }

    @Test
    public void testMaxAgePresent() throws Exception {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        // initiate AIA
        String queryString = "&" + KC_ACTION + "=" + UserModel.RequiredAction.UPDATE_PASSWORD.name();
        driver.navigate().to(driver.getCurrentUrl() + queryString);

        int maxAge = Integer.parseInt(getProviderLoginParam(OAuth2Constants.MAX_AGE));
        assertEquals(Constants.KC_ACTION_MAX_AGE, maxAge);
    }

    public String getProviderLoginParam(String paramName) throws Exception {
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        String currentUrl = driver.getCurrentUrl();
        log.debug("Current URL: " + currentUrl);

        return URLEncodedUtils.parse(new URI(currentUrl), Charset.defaultCharset())
                .stream()
                .filter(p -> paramName.equals(p.getName()))
                .map(NameValuePair::getValue)
                .findAny()
                .orElseThrow(() -> new AssertionError("Param was not found:" + paramName));
    }
}
