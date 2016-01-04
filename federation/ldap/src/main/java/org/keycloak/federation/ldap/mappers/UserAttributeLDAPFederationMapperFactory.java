package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAttributeLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "user-attribute-ldap-mapper";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty userModelAttribute = createConfigProperty(UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "User Model Attribute",
                "Name of mapped UserModel property or UserModel attribute in Keycloak DB. For example 'firstName', 'lastName, 'email', 'street' etc.", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(userModelAttribute);

        ProviderConfigProperty ldapAttribute = createConfigProperty(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "LDAP Attribute",
                "Name of mapped attribute on LDAP object. For example 'cn', 'sn, 'mail', 'street' etc.", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(ldapAttribute);

        ProviderConfigProperty readOnly = createConfigProperty(UserAttributeLDAPFederationMapper.READ_ONLY, "Read Only",
                "Read-only attribute is imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(readOnly);

        ProviderConfigProperty alwaysReadValueFromLDAP = createConfigProperty(UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "Always Read Value From LDAP",
                "If on, then during reading of the user will be value of attribute from LDAP always used instead of the value from Keycloak DB", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(alwaysReadValueFromLDAP);

        ProviderConfigProperty isMandatoryInLdap = createConfigProperty(UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "Is Mandatory In LDAP",
                "If true, attribute is mandatory in LDAP. Hence if there is no value in Keycloak DB, the empty value will be set to be propagated to LDAP", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(isMandatoryInLdap);
    }

    @Override
    public String getHelpText() {
        return "Used to map single attribute from LDAP user to attribute of UserModel in Keycloak DB";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        Map<String, String> defaultValues = new HashMap<>();
        LDAPConfig config = new LDAPConfig(providerModel.getConfig());

        String readOnly = config.getEditMode() == UserFederationProvider.EditMode.WRITABLE ? "false" : "true";
        defaultValues.put(UserAttributeLDAPFederationMapper.READ_ONLY, readOnly);

        defaultValues.put(UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false");
        defaultValues.put(UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");

        return defaultValues;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        checkMandatoryConfigAttribute(UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "User Model Attribute", mapperModel);
        checkMandatoryConfigAttribute(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "LDAP Attribute", mapperModel);
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new UserAttributeLDAPFederationMapper(mapperModel, federationProvider, realm);
    }
}
