package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.KeyPair;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractInitiateLogin implements AuthChallenge {
    protected static Logger log = Logger.getLogger(AbstractInitiateLogin.class);

    protected SamlDeployment deployment;
    protected SamlSessionStore sessionStore;

    public AbstractInitiateLogin(SamlDeployment deployment, SamlSessionStore sessionStore) {
        this.deployment = deployment;
        this.sessionStore = sessionStore;
    }

    @Override
    public int getResponseCode() {
        return 0;
    }

    @Override
    public boolean challenge(HttpFacade httpFacade) {
        try {
            String issuerURL = deployment.getEntityID();
            String nameIDPolicyFormat = deployment.getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                    .destination(deployment.getIDP().getSingleSignOnService().getRequestBindingUrl())
                    .issuer(issuerURL)
                    .forceAuthn(deployment.isForceAuthentication()).isPassive(deployment.isIsPassive())
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            if (deployment.getIDP().getSingleSignOnService().getResponseBinding() != null) {
                String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();
                if (deployment.getIDP().getSingleSignOnService().getResponseBinding() == SamlDeployment.Binding.POST) {
                    protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
                }
                authnRequestBuilder.protocolBinding(protocolBinding);

            }
            if (deployment.getAssertionConsumerServiceUrl() != null) {
                authnRequestBuilder.assertionConsumerUrl(deployment.getAssertionConsumerServiceUrl());
            }
            BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

            if (deployment.getIDP().getSingleSignOnService().signRequest()) {


                KeyPair keypair = deployment.getSigningKeyPair();
                if (keypair == null) {
                    throw new RuntimeException("Signing keys not configured");
                }
                if (deployment.getSignatureCanonicalizationMethod() != null) {
                    binding.canonicalizationMethod(deployment.getSignatureCanonicalizationMethod());
                }

                binding.signWith(keypair);
                binding.signDocument();
            }
            sessionStore.saveRequest();

            sendAuthnRequest(httpFacade, authnRequestBuilder, binding);
            sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.LOGGING_IN);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
        return true;
    }

    protected abstract void sendAuthnRequest(HttpFacade httpFacade, SAML2AuthnRequestBuilder authnRequestBuilder, BaseSAML2BindingBuilder binding) throws ProcessingException, ConfigurationException, IOException;

}
