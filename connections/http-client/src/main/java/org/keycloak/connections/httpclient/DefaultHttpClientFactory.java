package org.keycloak.connections.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.truststore.TruststoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.KeystoreUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultHttpClientFactory implements HttpClientFactory {

    private static final Logger logger = Logger.getLogger(DefaultHttpClientFactory.class);

    private volatile CloseableHttpClient httpClient;
    private Config.Scope config;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        lazyInit(session);

        return new HttpClientProvider() {
            @Override
            public HttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public void close() {

            }

            @Override
            public int postText(String uri, String text) throws IOException {
                HttpPost request = new HttpPost(uri);
                request.setEntity(EntityBuilder.create().setText(text).setContentType(ContentType.TEXT_PLAIN).build());
                HttpResponse response = httpClient.execute(request);
                try {
                    return response.getStatusLine().getStatusCode();
                } finally {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream is = entity.getContent();
                        if (is != null) is.close();
                    }

                }
            }

            @Override
            public InputStream get(String uri) throws IOException {
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                if (entity == null) return null;
                return entity.getContent();

            }
        };
    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {

        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    private void lazyInit(KeycloakSession session) {
        if (httpClient == null) {
            synchronized(this) {
                if (httpClient == null) {
                    long socketTimeout = config.getLong("socket-timeout-millis", -1L);
                    long establishConnectionTimeout = config.getLong("establish-connection-timeout-millis", -1L);
                    int maxPooledPerRoute = config.getInt("max-pooled-per-route", 0);
                    int connectionPoolSize = config.getInt("connection-pool-size", 200);
                    boolean disableCookies = config.getBoolean("disable-cookies", true);
                    String clientKeystore = config.get("client-keystore");
                    String clientKeystorePassword = config.get("client-keystore-password");
                    String clientPrivateKeyPassword = config.get("client-key-password");

                    TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
                    boolean disableTrustManager = truststoreProvider == null || truststoreProvider.getTruststore() == null;
                    if (disableTrustManager) {
                        logger.warn("Truststore is disabled");
                    }
                    HttpClientBuilder.HostnameVerificationPolicy hostnamePolicy = disableTrustManager ? null
                            : HttpClientBuilder.HostnameVerificationPolicy.valueOf(truststoreProvider.getPolicy().name());

                    HttpClientBuilder builder = new HttpClientBuilder();
                    builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                            .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
                            .maxPooledPerRoute(maxPooledPerRoute)
                            .connectionPoolSize(connectionPoolSize)
                            .disableCookies(disableCookies);

                    if (disableTrustManager) {
                        // TODO: is it ok to do away with disabling trust manager?
                        //builder.disableTrustManager();
                    } else {
                        builder.hostnameVerification(hostnamePolicy);
                        try {
                            builder.trustStore(truststoreProvider.getTruststore());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load truststore", e);
                        }
                    }

                    if (clientKeystore != null) {
                        clientKeystore = EnvUtil.replace(clientKeystore);
                        try {
                            KeyStore clientCertKeystore = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                            builder.keyStore(clientCertKeystore, clientPrivateKeyPassword);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load keystore", e);
                        }
                    }
                    httpClient = builder.build();
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }



}
