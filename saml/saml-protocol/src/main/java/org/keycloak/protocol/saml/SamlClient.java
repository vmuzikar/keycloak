package org.keycloak.protocol.saml;

import org.keycloak.models.ClientConfigResolver;
import org.keycloak.models.ClientModel;
import org.keycloak.saml.SignatureAlgorithm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlClient extends ClientConfigResolver {

    public SamlClient(ClientModel client) {
        super(client);
    }

    public String getCanonicalizationMethod() {
        return resolveAttribute(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
    }

    public void setCanonicalizationMethod(String value) {
        client.setAttribute(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE, value);
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = resolveAttribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM);
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null)
                return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm algorithm) {
        client.setAttribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, algorithm.name());
    }

    public String getNameIDFormat() {
        return resolveAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE);
    }
    public void setNameIDFormat(String format) {
        client.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, format);
    }

    public boolean includeAuthnStatement() {
        return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT));
    }

    public void setIncludeAuthnStatement(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, Boolean.toString(val));
    }

    public boolean forceNameIDFormat() {
        return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE));

    }
    public void setForceNameIDFormat(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, Boolean.toString(val));
    }

    public boolean requiresRealmSignature() {
        return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE));
    }

    public void setRequiresRealmSignature(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.toString(val));

    }

    public boolean forcePostBinding() {
        return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING));
    }

    public void setForcePostBinding(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.toString(val));

    }
    public boolean requiresAssertionSignature() {
       return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE));
    }

    public void setRequiresAssertionSignature(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE   , Boolean.toString(val));

    }
    public boolean requiresEncryption() {
       return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_ENCRYPT));
    }


    public void setRequiresEncryption(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(val));

    }

    public boolean requiresClientSignature() {
        return "true".equals(resolveAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE));
    }

    public void setRequiresClientSignature(boolean val) {
        client.setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE   , Boolean.toString(val));

    }

    public String getClientSigningCertificate() {
        return client.getAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
    }

    public void setClientSigningCertificate(String val) {
        client.setAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, val);

    }

    public String getClientSigningPrivateKey() {
        return client.getAttribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY);
    }

    public void setClientSigningPrivateKey(String val) {
        client.setAttribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, val);

    }

    public String getClientEncryptingCertificate() {
        return client.getAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE);
    }

    public void setClientEncryptingCertificate(String val) {
        client.setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, val);

    }
    public String getClientEncryptingPrivateKey() {
        return client.getAttribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE);
    }

    public void setClientEncryptingPrivateKey(String val) {
        client.setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, val);

    }

}
