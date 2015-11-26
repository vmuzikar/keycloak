package org.keycloak.client.registration;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
class HttpUtil {

    private HttpClient httpClient;

    private String baseUri;

    private Auth auth;

    HttpUtil(HttpClient httpClient, String baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    void setAuth(Auth auth) {
        this.auth = auth;
    }

    InputStream doPost(String content, String contentType, String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpPost request = new HttpPost(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            request.setEntity(new StringEntity(content));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 201) {
                return responseStream;
            } else {
                responseStream.close();
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    InputStream doGet(String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpGet request = new HttpGet(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.ACCEPT, acceptType);

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 200) {
                return responseStream;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                responseStream.close();
                return null;
            } else {
                if (responseStream != null) {
                    responseStream.close();
                }
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    InputStream doPut(String content, String contentType, String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpPut request = new HttpPut(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            request.setEntity(new StringEntity(content));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            if (response.getEntity() != null) {
                response.getEntity().getContent();
            }

            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 200) {
                return responseStream;
            } else {
                if (responseStream != null) {
                    responseStream.close();
                }
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    void doDelete(String... path) throws ClientRegistrationException {
        try {
            HttpDelete request = new HttpDelete(getUrl(baseUri, path));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }

            if (response.getStatusLine().getStatusCode() != 204) {
                throw new HttpErrorException(response.getStatusLine());
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    void close() throws ClientRegistrationException {
        if (httpClient instanceof CloseableHttpClient) {
            try {
                ((CloseableHttpClient) httpClient).close();
            } catch (IOException e) {
                throw new ClientRegistrationException("Failed to close http client", e);
            }
        }
    }

    static String getUrl(String baseUri, String... path) {
        StringBuilder s = new StringBuilder();
        s.append(baseUri);
        for (String p : path) {
            s.append('/');
            s.append(p);
        }
        return s.toString();
    }

    private void addAuth(HttpRequestBase request) {
        if (auth != null) {
            auth.addAuth(request);
        }
    }

}
