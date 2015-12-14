package org.keycloak.testsuite.auth.page;

import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Keycloak realm.
 * <p>
 * URL: http://localhost:${auth.server.http.port}/auth/realms/{authRealm}
 *
 * @author tkyjovsk
 */
public class AuthRealm extends AuthServer implements PageWithLoginUrl {

    public static final String AUTH_REALM = "authRealm";

    public static final String MASTER = "master";
    public static final String TEST = "test";
    public static final String DEMO = "demo";
    public static final String EXAMPLE = "example";
    public static final String SAMLDEMO = "saml-demo";
    public static final String SAMLSERVLETDEMO = "demo";

    public static final String ADMIN = "admin";

    public AuthRealm() {
        setUriParameter(AUTH_REALM, MASTER);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("realms/{" + AUTH_REALM + "}");
    }

    public void setAuthRealm(String authRealm) {
        setUriParameter(AUTH_REALM, authRealm);
    }

    public void setAuthRealm(AuthRealm authRealm) {
        setUriParameter(AUTH_REALM, authRealm.getAuthRealm());
    }

    public String getAuthRealm() {
        return (String) getUriParameter(AUTH_REALM);
    }

    /**
     * @return OIDC Login URL for authRealm
     */
    @Override
    public URI getOIDCLoginUrl() {
        return OIDCLoginProtocolService.authUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAuthRealm());
    }

    public URI getOIDCLogoutUrl() {
        return OIDCLoginProtocolService.logoutUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAuthRealm());
    }

    public URI getOIDCTokenUrl() {
        return OIDCLoginProtocolService.tokenUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAuthRealm());
    }
}
