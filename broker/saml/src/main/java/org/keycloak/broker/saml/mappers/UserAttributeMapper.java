package org.keycloak.broker.saml.mappers;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAttributeMapper extends AbstractIdentityProviderMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String USER_ATTRIBUTE = "user.attribute";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Attribute Name");
        property.setHelpText("Name of attribute to search for in assertion.  You can leave this blank and specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Friendly Name");
        property.setHelpText("Friendly name of attribute to search for in assertion.  You can leave this blank and specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store saml attribute.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-user-attribute-idp-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        Object value = getAttribute(mapperModel, context);
        if (value != null) {
            context.setUserAttribute(attribute, value.toString());
        }
    }

    protected String getAttribute(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String name = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (name != null && name.trim().equals("")) name = null;
        String friendly = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        if (friendly != null && friendly.trim().equals("")) friendly = null;
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        for (AttributeStatementType statement : assertion.getAttributeStatements()) {
            for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                AttributeType attr = choice.getAttribute();
                if (name != null && !name.equals(attr.getName())) continue;
                if (friendly != null && !friendly.equals(attr.getFriendlyName())) continue;

                List<Object> attributeValue = attr.getAttributeValue();
                if (attributeValue == null || attributeValue.isEmpty()) return null;
                return attributeValue.get(0).toString();
            }
        }
        return null;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        Object value = getAttribute(mapperModel, context);
        String current = user.getFirstAttribute(attribute);
        if (value != null && !value.equals(current)) {
            user.setSingleAttribute(attribute, value.toString());
        } else if (value == null) {
            user.removeAttribute(attribute);
        }

    }

    @Override
    public String getHelpText() {
        return "Import declared saml attribute if it exists in assertion into the specified user attribute.";
    }

}
