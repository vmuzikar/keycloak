package org.keycloak.adapters.saml;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.GenericPrincipalFactory;
import org.keycloak.saml.common.exceptions.ParsingException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keycloak authentication valve
 * 
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSamlAuthenticatorValve extends FormAuthenticator implements LifecycleListener {

    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";

	private final static Logger log = Logger.getLogger(""+AbstractSamlAuthenticatorValve.class);
	protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected SamlDeploymentContext deploymentContext;
    protected SessionIdMapper mapper = new InMemorySessionIdMapper();

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            cache = false;
        } else if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
        	keycloakInit();
        } else if (event.getType() == Lifecycle.BEFORE_STOP_EVENT) {
            beforeStop();
        }
    }

    protected void logoutInternal(Request request) {
        CatalinaHttpFacade facade = new CatalinaHttpFacade(null, request);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        SamlSessionStore tokenStore = getTokenStore(request, facade, deployment);
        tokenStore.logoutAccount();
        request.setUserPrincipal(null);
    }

    @SuppressWarnings("UseSpecificCatch")
    public void keycloakInit() {
        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exists, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured

        String configResolverClass = context.getServletContext().getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                throw new RuntimeException("Not implemented yet");
                //KeycloakConfigResolver configResolver = (KeycloakConfigResolver) context.getLoader().getClassLoader().loadClass(configResolverClass).newInstance();
                //deploymentContext = new SamlDeploymentContext(configResolver);
                //log.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.log(Level.FINE, "The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                //deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream is = getConfigInputStream(context);
            final SamlDeployment deployment;
            if (is == null) {
                log.info("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                deployment = new DefaultSamlDeployment();
            } else {
                try {
                    ResourceLoader loader = new ResourceLoader() {
                        @Override
                        public InputStream getResourceAsStream(String resource) {
                            return context.getServletContext().getResourceAsStream(resource);
                        }
                    };
                    deployment = new DeploymentBuilder().build(is, loader);
                } catch (ParsingException e) {
                    throw new RuntimeException(e);
                }
            }
            deploymentContext = new SamlDeploymentContext(deployment);
            log.fine("Keycloak is using a per-deployment configuration.");
        }

        context.getServletContext().setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);
    }

    protected void beforeStop() {
    }

    private static InputStream getConfigFromServletContext(ServletContext servletContext) {
        String xml = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (xml == null) {
            return null;
        }
        log.finest("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.finest(xml);
        return new ByteArrayInputStream(xml.getBytes());
    }

    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getConfigFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.fine("**** using /WEB-INF/keycloak-saml.xml");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak-saml.xml");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    log.log(Level.SEVERE, "NOT FOUND {0}", path);
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.fine("*********************** SAML ************");
        try {
            super.invoke(request, response);
        } finally {
        }
    }

    protected abstract GenericPrincipalFactory createPrincipalFactory();
    protected abstract boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException;
    protected void forwardToLogoutPage(Request request, HttpServletResponse response,SamlDeployment deployment) {
        RequestDispatcher disp = request.getRequestDispatcher(deployment.getLogoutPage());
        //make sure the login page is never cached
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");


        try {
            disp.forward(request.getRequest(), response);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    protected boolean authenticateInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException {
        log.fine("authenticateInternal");
        CatalinaHttpFacade facade = new CatalinaHttpFacade(response, request);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.fine("deployment not configured");
            return false;
        }
        SamlSessionStore tokenStore = getTokenStore(request, facade, deployment);


        CatalinaSamlAuthenticator authenticator = new CatalinaSamlAuthenticator(facade, deployment, tokenStore);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.fine("AUTHENTICATED");
            if (facade.isEnded()) {
                return false;
            }
            return true;
        }
        if (outcome == AuthOutcome.LOGGED_OUT) {
            logoutInternal(request);
            if (deployment.getLogoutPage() != null) {
                forwardToLogoutPage(request, response, deployment);

            }
            log.fine("Logging OUT");
            return false;
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            log.fine("challenge");
            if (loginConfig == null) {
                loginConfig = request.getContext().getLoginConfig();
            }
            challenge.challenge(facade);
        }
        return false;
    }

    public void keycloakSaveRequest(Request request) throws IOException {
        saveRequest(request, request.getSessionInternal(true));
    }

    public boolean keycloakRestoreRequest(Request request) {
        try {
            return restoreRequest(request, request.getSessionInternal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected SamlSessionStore getTokenStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        SamlSessionStore store = (SamlSessionStore)request.getNote(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }

        store = new CatalinaSamlSessionStore(userSessionManagement, createPrincipalFactory(), mapper, request, this, facade);

        request.setNote(TOKEN_STORE_NOTE, store);
        return store;
    }

}
