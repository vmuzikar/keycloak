package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;

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
}
