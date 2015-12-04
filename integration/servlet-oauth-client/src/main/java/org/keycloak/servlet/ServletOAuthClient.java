package org.keycloak.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.security.cert.X509Certificate;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClient extends KeycloakDeploymentDelegateOAuthClient {

    /**
     * closes client
     */
    public void stop() {
        getDeployment().getClient().getConnectionManager().shutdown();
    }

    private AccessTokenResponse resolveBearerToken(HttpServletRequest request, String redirectUri, String code) throws IOException, ServerRequest.HttpFailure {
        // Don't send sessionId in oauth clients for now
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);
        return ServerRequest.invokeAccessCodeToToken(resolvedDeployment, code, redirectUri, null);
    }

    /**
     * Start the process of obtaining an access token by redirecting the browser
     * to the authentication server
     *
     * @param relativePath path relative to context root you want auth server to redirect back to
     * @param request
     * @param response
     * @throws IOException
     */
    public void redirectRelative(String relativePath, HttpServletRequest request, HttpServletResponse response) throws IOException {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(request.getRequestURL().toString())
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .path(relativePath);
        String redirect = builder.toTemplate();
        redirect(redirect, request, response);
    }


    /**
     * Start the process of obtaining an access token by redirecting the browser
     * to the authentication server
     *
     * @param redirectUri full URI you want auth server to redirect back to
     * @param request
     * @param response
     * @throws IOException
     */
    public void redirect(String redirectUri, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = getStateCode();
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);
        String authUrl = resolvedDeployment.getAuthUrl().clone().build().toString();

        KeycloakUriBuilder uriBuilder =  KeycloakUriBuilder.fromUri(authUrl)
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getClientId())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state);
        if (scope != null) {
            uriBuilder.queryParam(OAuth2Constants.SCOPE, scope);
        }
        URI url = uriBuilder.build();

        String stateCookiePath = this.stateCookiePath;
        if (stateCookiePath == null) stateCookiePath = request.getContextPath();
        if (stateCookiePath.equals("")) stateCookiePath = "/";

        Cookie cookie = new Cookie(stateCookieName, state);
        cookie.setSecure(isSecure);
        cookie.setPath(stateCookiePath);
        response.addCookie(cookie);
        response.sendRedirect(url.toString());
    }

    protected String getCookieValue(String name, HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    protected String getCode(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) return null;
        String[] params = query.split("&");
        for (String param : params) {
            int eq = param.indexOf('=');
            if (eq == -1) continue;
            String name = param.substring(0, eq);
            if (!name.equals(OAuth2Constants.CODE)) continue;
            return param.substring(eq + 1);
        }
        return null;
    }


    /**
     * Obtain the code parameter from the url after being redirected back from the auth-server.  Then
     * do an authenticated request back to the auth-server to turn the access code into an access token.
     *
     * @param request
     * @return
     * @throws IOException
     * @throws org.keycloak.adapters.ServerRequest.HttpFailure
     */
    public AccessTokenResponse getBearerToken(HttpServletRequest request) throws IOException, ServerRequest.HttpFailure {
        String error = request.getParameter(OAuth2Constants.ERROR);
        if (error != null) throw new IOException("OAuth error: " + error);
        String redirectUri = request.getRequestURL().append("?").append(request.getQueryString()).toString();
        String stateCookie = getCookieValue(stateCookieName, request);
        if (stateCookie == null) throw new IOException("state cookie not set");
        // we can call get parameter as this should be a redirect
        String state = request.getParameter(OAuth2Constants.STATE);
        String code = request.getParameter(OAuth2Constants.CODE);

        if (state == null) throw new IOException("state parameter was null");
        if (!state.equals(stateCookie)) {
            throw new IOException("state parameter invalid");
        }
        if (code == null) throw new IOException("code parameter was null");
        return resolveBearerToken(request, redirectUri, code);
    }

    public AccessTokenResponse refreshToken(HttpServletRequest request, String refreshToken) throws IOException, ServerRequest.HttpFailure {
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);
        return ServerRequest.invokeRefresh(resolvedDeployment, refreshToken);
    }

    public static IDToken extractIdToken(String idToken) {
        if (idToken == null) return null;
        try {
            JWSInput input = new JWSInput(idToken);
            return input.readJsonContent(IDToken.class);
        } catch (JWSInputException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakDeployment resolveDeployment(KeycloakDeployment baseDeployment, HttpServletRequest request) {
        ServletFacade facade = new ServletFacade(request);
        return new AdapterDeploymentContext(baseDeployment).resolveDeployment(facade);
    }


    public static class ServletFacade implements OIDCHttpFacade {

        private final HttpServletRequest servletRequest;

        private ServletFacade(HttpServletRequest servletRequest) {
            this.servletRequest = servletRequest;
        }

        @Override
        public KeycloakSecurityContext getSecurityContext() {
            throw new IllegalStateException("Not yet implemented");
        }

        @Override
        public Request getRequest() {
            return new Request() {

                @Override
                public String getFirstParam(String param) {
                    return servletRequest.getParameter(param);
                }

                @Override
                public String getMethod() {
                    return servletRequest.getMethod();
                }

                @Override
                public String getURI() {
                    return servletRequest.getRequestURL().toString();
                }

                @Override
                public boolean isSecure() {
                    return servletRequest.isSecure();
                }

                @Override
                public String getQueryParamValue(String param) {
                    return servletRequest.getParameter(param);
                }

                @Override
                public Cookie getCookie(String cookieName) {
                    // TODO
                    return null;
                }

                @Override
                public String getHeader(String name) {
                    return servletRequest.getHeader(name);
                }

                @Override
                public List<String> getHeaders(String name) {
                    // TODO
                    return null;
                }

                @Override
                public InputStream getInputStream() {
                    try {
                        return servletRequest.getInputStream();
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }

                @Override
                public String getRemoteAddr() {
                    return servletRequest.getRemoteAddr();
                }

                @Override
                public void setError(AuthenticationError error) {
                    servletRequest.setAttribute(AuthenticationError.class.getName(), error);

                }

                @Override
                public void setError(LogoutError error) {
                    servletRequest.setAttribute(LogoutError.class.getName(), error);
                }

            };
        }

        @Override
        public Response getResponse() {
            throw new IllegalStateException("Not yet implemented");
        }

        @Override
        public X509Certificate[] getCertificateChain() {
            throw new IllegalStateException("Not yet implemented");
        }
    }
}
