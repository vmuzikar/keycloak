package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel extends RoleContainerModel,  ProtocolMapperContainerModel, ScopeContainerModel {

    // COMMON ATTRIBUTES

    String PRIVATE_KEY = "privateKey";
    String PUBLIC_KEY = "publicKey";
    String X509CERTIFICATE = "X509Certificate";

    void updateClient();

    String getId();

    String getClientId();

    void setClientId(String clientId);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSurrogateAuthRequired();

    void setSurrogateAuthRequired(boolean surrogateAuthRequired);

    Set<String> getWebOrigins();

    void setWebOrigins(Set<String> webOrigins);

    void addWebOrigin(String webOrigin);

    void removeWebOrigin(String webOrigin);

    Set<String> getRedirectUris();

    void setRedirectUris(Set<String> redirectUris);

    void addRedirectUri(String redirectUri);

    void removeRedirectUri(String redirectUri);

    String getManagementUrl();

    void setManagementUrl(String url);

    String getRootUrl();

    void setRootUrl(String url);

    String getBaseUrl();

    void setBaseUrl(String url);

    List<String> getDefaultRoles();

    void addDefaultRole(String name);

    void updateDefaultRoles(String[] defaultRoles);


    boolean isBearerOnly();
    void setBearerOnly(boolean only);

    int getNodeReRegistrationTimeout();

    void setNodeReRegistrationTimeout(int timeout);

    String getClientAuthenticatorType();
    void setClientAuthenticatorType(String clientAuthenticatorType);

    boolean validateSecret(String secret);
    String getSecret();
    public void setSecret(String secret);

    String getRegistrationToken();
    void setRegistrationToken(String registrationToken);

    String getProtocol();
    void setProtocol(String protocol);

    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();

    boolean isFrontchannelLogout();
    void setFrontchannelLogout(boolean flag);


    boolean isPublicClient();
    void setPublicClient(boolean flag);

    boolean isConsentRequired();
    void setConsentRequired(boolean consentRequired);

    boolean isStandardFlowEnabled();
    void setStandardFlowEnabled(boolean standardFlowEnabled);

    boolean isImplicitFlowEnabled();
    void setImplicitFlowEnabled(boolean implicitFlowEnabled);

    boolean isDirectAccessGrantsEnabled();
    void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled);

    boolean isServiceAccountsEnabled();
    void setServiceAccountsEnabled(boolean serviceAccountsEnabled);

    RealmModel getRealm();

    ClientTemplateModel getClientTemplate();
    void setClientTemplate(ClientTemplateModel template);
    boolean useTemplateScope();
    void setUseTemplateScope(boolean flag);
    boolean useTemplateMappers();
    void setUseTemplateMappers(boolean flag);
    boolean useTemplateConfig();
    void setUseTemplateConfig(boolean flag);

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

     Map<String, Integer> getRegisteredNodes();

    /**
     * Register node or just update the 'lastReRegistration' time if this node is already registered
     *
     * @param nodeHost
     * @param registrationTime
     */
    void registerNode(String nodeHost, int registrationTime);

    void unregisterNode(String nodeHost);
}
