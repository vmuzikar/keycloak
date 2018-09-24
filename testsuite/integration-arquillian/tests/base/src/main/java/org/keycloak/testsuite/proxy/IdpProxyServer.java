/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdpProxyServer {
    public static final int PROXY_PORT = 9090;
    public static final String IDP_HOST = "mocked-idp";
    public static final String IDP_AUTH_URL = "http://" + IDP_HOST + "/auth";
    public static final String IDP_TOKEN_URL = "http://" + IDP_HOST + "/token";
    public static final String OAUTH_CODE = "doesnt-matter";

    private HttpProxyServer proxy;
    private HttpProxyServerBootstrap proxyBootstrap;

    public static void main(String[] args) {
        new IdpProxyServer().start();
    }

    public IdpProxyServer() {
        proxyBootstrap = DefaultHttpProxyServer.bootstrap()
                .withPort(PROXY_PORT)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                final String uri = originalRequest.getUri();

                                if (uri.startsWith(IDP_AUTH_URL)) {
                                    String redirectUri = getParamFromUri(OAuth2Constants.REDIRECT_URI);
                                    redirectUri += "?state=fake-state&code=" + OAUTH_CODE;

                                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
                                    HttpHeaders.setHeader(response, HttpHeaders.Names.LOCATION, redirectUri);
                                    return response;
                                }
                                else if (uri.startsWith(IDP_TOKEN_URL)) {
                                    if (!getParamFromUri(OAuth2Constants.CODE).equals(OAUTH_CODE)) {
                                        throw new RuntimeException("Unexpected code value!");
                                    }

                                    IDToken idToken = new IDToken();
                                    idToken.setPreferredUsername("vmuzikar");
                                    idToken.setName("Václav Muzikář");
                                    idToken.setEmail("vmuzikar@redhat.com");
                                    String idTokenJwt = new JWSBuilder().type("JWT").jsonContent(idToken).none();
                                    String accessTokenJwt = new JWSBuilder().type("JWT").jsonContent(new AccessToken()).none();

                                    AccessTokenResponse tokenResponse = new AccessTokenResponse();
                                    tokenResponse.setToken(accessTokenJwt);
                                    tokenResponse.setIdToken(idTokenJwt);

                                    ByteBuf buffer;
                                    try {
                                        buffer = Unpooled.wrappedBuffer(JsonSerialization.writeValueAsBytes(tokenResponse));
                                    }
                                    catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                                    HttpHeaders.setContentLength(response, buffer.readableBytes());
                                    HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON);
                                    return response;
                                }
                                else {
                                    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
                                }
                            }

                            private String getParamFromUri(final String paramName) {
                                Matcher matcher = Pattern.compile(".*" + paramName + "=([^&]+).*").matcher(originalRequest.getUri());
                                matcher.find();
                                return URLDecoder.decode(matcher.group(1));
                            }
                        };
                    }
                });
    }

    public void start() {
        if (proxy != null) {
            throw new IllegalStateException("Proxy already started");
        }
        proxy = proxyBootstrap.start();
    }

    public void stop() {
        if (proxy == null) {
            throw new IllegalStateException("Proxy has not been started yet");
        }
        proxy.stop();
        proxy = null;
    }
}
