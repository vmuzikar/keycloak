package org.keycloak.adapters.springsecurity.authentication;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Logs the current user out of Keycloak.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(KeycloakLogoutHandler.class);

    private AdapterDeploymentContext adapterDeploymentContext;

    public KeycloakLogoutHandler(AdapterDeploymentContext adapterDeploymentContext) {
        Assert.notNull(adapterDeploymentContext);
        this.adapterDeploymentContext = adapterDeploymentContext;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null) {
            log.warn("Cannot log out without authentication");
            return;
        }
        else if (!KeycloakAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            log.warn("Cannot log out a non-Keycloak authentication: {}", authentication);
            return;
        }

        handleSingleSignOut(request, response, (KeycloakAuthenticationToken) authentication);
    }

    protected void handleSingleSignOut(HttpServletRequest request, HttpServletResponse response, KeycloakAuthenticationToken authenticationToken) {
        HttpFacade facade = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) authenticationToken.getAccount().getKeycloakSecurityContext();
        session.logout(deployment);
    }
}
