package org.keycloak.broker.oidc.mappers;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Abstract class for Social Provider mappers which allow mapping of JSON user profile field into Keycloak user
 * attribute. Concrete mapper classes with own ID and provider mapping must be implemented for each social provider who
 * uses {@link JsonNode} user profile.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class AbstractJsonUserAttributeMapper extends AbstractIdentityProviderMapper {


	protected static final Logger logger = Logger.getLogger(AbstractJsonUserAttributeMapper.class);

	protected static final Logger LOGGER_DUMP_USER_PROFILE = Logger.getLogger("org.keycloak.social.user_profile_dump");

	private static final String JSON_PATH_DELIMITER = ".";

	/**
	 * Config param where name of mapping source JSON User Profile field is stored.
	 */
	public static final String CONF_JSON_FIELD = "jsonField";
	/**
	 * Config param where name of mapping target USer attribute is stored.
	 */
	public static final String CONF_USER_ATTRIBUTE = "userAttribute";

	/**
	 * Key in {@link BrokeredIdentityContext#getContextData()} where {@link JsonNode} with user profile is stored.
	 */
	public static final String CONTEXT_JSON_NODE = OIDCIdentityProvider.USER_INFO;

	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

	static {
		ProviderConfigProperty property;
		ProviderConfigProperty property1;
		property1 = new ProviderConfigProperty();
		property1.setName(CONF_JSON_FIELD);
		property1.setLabel("Social Profile JSON Field Path");
		property1.setHelpText("Path of field in Social provider User Profile JSON data to get value from. You can use dot notation for nesting and square brackets for array index. Eg. 'contact.address[0].country'.");
		property1.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property1);
		property = new ProviderConfigProperty();
		property.setName(CONF_USER_ATTRIBUTE);
		property.setLabel("User Attribute Name");
		property.setHelpText("User attribute name to store information into.");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		configProperties.add(property);
	}

	/**
	 * Store used profile JsonNode into user context for later use by this mapper. Profile data are dumped into special logger if enabled also to allow investigation of the structure. 
	 * 
	 * @param user context to store profile data into
	 * @param profile to store into context
	 * @param provider identification of social provider to be used in log dump
	 *
	 * @see #preprocessFederatedIdentity(KeycloakSession, RealmModel, IdentityProviderMapperModel, BrokeredIdentityContext)
	 * @see BrokeredIdentityContext#getContextData()
	 */
	public static void storeUserProfileForMapper(BrokeredIdentityContext user, JsonNode profile, String provider) {
		user.getContextData().put(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE, profile);
		if (LOGGER_DUMP_USER_PROFILE.isDebugEnabled())
			LOGGER_DUMP_USER_PROFILE.debug("User Profile JSON Data for provider "+provider+": " + profile);
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
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
	public String getHelpText() {
		return "Import user profile information if it exists in Social provider JSON data into the specified user attribute.";
	}

	@Override
	public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		String attribute = mapperModel.getConfig().get(CONF_USER_ATTRIBUTE);
		if (attribute == null || attribute.trim().isEmpty()) {
			logger.warnf("Attribute is not configured for mapper %s", mapperModel.getName());
			return;
		}
		attribute = attribute.trim();

		String value = getJsonValue(mapperModel, context);
		if (value != null) {
			context.setUserAttribute(attribute, value);
		}
	}

	@Override
	public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		// we do not update user profile from social provider
	}

	protected static String getJsonValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

		String jsonField = mapperModel.getConfig().get(CONF_JSON_FIELD);
		if (jsonField == null || jsonField.trim().isEmpty()) {
			logger.warnf("JSON field path is not configured for mapper %s", mapperModel.getName());
			return null;
		}
		jsonField = jsonField.trim();

		if (jsonField.startsWith(JSON_PATH_DELIMITER) || jsonField.endsWith(JSON_PATH_DELIMITER) || jsonField.startsWith("[")) {
			logger.warnf("JSON field path is invalid %s", jsonField);
			return null;
		}

		JsonNode profileJsonNode = (JsonNode) context.getContextData().get(CONTEXT_JSON_NODE);

		String value = getJsonValue(profileJsonNode, jsonField);

		if (value == null) {
			logger.debugf("User profile JSON value '%s' is not available.", jsonField);
		}

		return value;
	}

	protected static String getJsonValue(JsonNode baseNode, String fieldPath) {
		logger.debug("Going to process JsonNode path " + fieldPath + " on data " + baseNode);
		if (baseNode != null) {

			int idx = fieldPath.indexOf(JSON_PATH_DELIMITER);

			String currentFieldName = fieldPath;
			if (idx > 0) {
				currentFieldName = fieldPath.substring(0, idx).trim();
				if (currentFieldName.isEmpty()) {
					logger.debug("JSON path is invalid " + fieldPath);
					return null;
				}
			}

			String currentNodeName = currentFieldName;
			int arrayIndex = -1;
			if (currentFieldName.endsWith("]")) {
				int bi = currentFieldName.indexOf("[");
				if (bi == -1) {
					logger.debug("Invalid array index construct in " + currentFieldName);
					return null;
				}
				try {
				String is = currentFieldName.substring(bi+1, currentFieldName.length() - 1).trim();
					arrayIndex = Integer.parseInt(is);
				} catch (Exception e) {
					logger.debug("Invalid array index construct in " + currentFieldName);
					return null;
				}
				currentNodeName = currentFieldName.substring(0,bi).trim();
			}

			JsonNode currentNode = baseNode.get(currentNodeName);
			if (arrayIndex > -1 && currentNode.isArray()) {
				logger.debug("Going to take array node at index " + arrayIndex);
				currentNode = currentNode.get(arrayIndex);
			}

			if (currentNode == null) {
				logger.debug("JsonNode not found for name " + currentFieldName);
				return null;
			}

			if (idx < 0) {
				if (!currentNode.isValueNode()) {
					logger.debug("JsonNode is not value node for name " + currentFieldName);
					return null;
				}
				String ret = currentNode.asText();
				if (ret != null && !ret.trim().isEmpty())
					return ret.trim();
			} else {
				return getJsonValue(currentNode, fieldPath.substring(idx + 1));
			}
		}
		return null;
	}

}
