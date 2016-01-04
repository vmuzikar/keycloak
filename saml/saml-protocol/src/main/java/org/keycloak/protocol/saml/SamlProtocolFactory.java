package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolFactory extends AbstractLoginProtocolFactory {

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new SamlService(realm, event, authManager);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        //PicketLinkCoreSTS sts = PicketLinkCoreSTS.instance();
        //sts.installDefaultConfiguration();
    }

    @Override
    public String getId() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        ProtocolMapperModel model;
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 email",
                "email",
                X500SAMLProfileConstants.EMAIL.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.EMAIL.getFriendlyName(),
                true, "${email}");
        builtins.add(model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 givenName",
                "firstName",
                X500SAMLProfileConstants.GIVEN_NAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(),
                true, "${givenName}");
        builtins.add(model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 surname",
                "lastName",
                X500SAMLProfileConstants.SURNAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.SURNAME.getFriendlyName(),
                true, "${familyName}");
        builtins.add(model);
        model = RoleListMapper.create("role list", "Role", AttributeStatementHelper.BASIC, null, false);
        builtins.add(model);
        defaultBuiltins.add(model);

    }


    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) {
            model.setProtocol(getId());
            client.addProtocolMapper(model);
        }
    }

    @Override
    public void setupClientDefaults(ClientRepresentation clientRep, ClientModel newClient) {
        SamlClientRepresentation rep = new SamlClientRepresentation(clientRep);
        SamlClient client = new SamlClient(newClient);
        if (clientRep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
        if (rep.getCanonicalizationMethod() == null) {
            client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        }
        if (rep.getSignatureAlgorithm() == null) {
            client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        }

        if (rep.getNameIDFormat() == null) {
            client.setNameIDFormat("username");
        }

        if (rep.getIncludeAuthnStatement() == null) {
            client.setIncludeAuthnStatement(true);
        }

        if (rep.getForceNameIDFormat() == null) {
            client.setForceNameIDFormat(false);
        }

        if (rep.getSamlServerSignature() == null) {
            client.setRequiresRealmSignature(true);
        }
        if (rep.getForcePostBinding() == null) {
            client.setForcePostBinding(true);
        }

        if (rep.getClientSignature() == null) {
            client.setRequiresClientSignature(true);
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(newClient.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());
        }

        if (clientRep.isFrontchannelLogout() == null) {
            newClient.setFrontchannelLogout(true);
        }
    }
}
