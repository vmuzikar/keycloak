package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.ProtocolMapperEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAdapter extends AbstractMongoAdapter<MongoClientEntity> implements ClientModel {

    protected final MongoClientEntity clientEntity;
    private final RealmModel realm;
    protected  KeycloakSession session;

    public ClientAdapter(KeycloakSession session, RealmModel realm, MongoClientEntity clientEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.session = session;
        this.realm = realm;
        this.clientEntity = clientEntity;
    }

    @Override
    public MongoClientEntity getMongoEntity() {
        return clientEntity;
    }

    @Override
    public void updateClient() {
        updateMongoEntity();
    }


    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntity().getClientId();
    }

    @Override
    public String getName() {
        return getMongoEntity().getName();
    }

    @Override
    public void setName(String name) {
        getMongoEntity().setName(name);
        updateMongoEntity();
    }

    @Override
    public String getDescription() { return getMongoEntity().getDescription(); }

    @Override
    public void setDescription(String description) {
        getMongoEntity().setDescription(description);
        updateMongoEntity();
    }

    @Override
    public void setClientId(String clientId) {
        getMongoEntity().setClientId(clientId);
        updateMongoEntity();
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntity().getWebOrigins() != null) {
            result.addAll(getMongoEntity().getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        getMongoEntity().setWebOrigins(result);
        updateMongoEntity();
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getMongoStore().pushItemToList(clientEntity, "webOrigins", webOrigin, true, invocationContext);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getMongoStore().pullItemFromList(clientEntity, "webOrigins", webOrigin, invocationContext);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntity().getRedirectUris() != null) {
            result.addAll(getMongoEntity().getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        getMongoEntity().setRedirectUris(result);
        updateMongoEntity();
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getMongoStore().pushItemToList(clientEntity, "redirectUris", redirectUri, true, invocationContext);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getMongoStore().pullItemFromList(clientEntity, "redirectUris", redirectUri, invocationContext);
    }

    @Override
    public boolean isEnabled() {
        return getMongoEntity().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getMongoEntity().setEnabled(enabled);
        updateMongoEntity();
    }

    @Override
    public String getClientAuthenticatorType() {
        return getMongoEntity().getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getMongoEntity().setClientAuthenticatorType(clientAuthenticatorType);
        updateMongoEntity();
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(getMongoEntity().getSecret());
    }

    @Override
    public String getSecret() {
        return getMongoEntity().getSecret();
    }

    @Override
    public void setSecret(String secret) {
        getMongoEntity().setSecret(secret);
        updateMongoEntity();
    }

    @Override
    public String getRegistrationToken() {
        return getMongoEntity().getRegistrationToken();
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        getMongoEntity().setRegistrationToken(registrationToken);
        updateMongoEntity();
    }

    @Override
    public boolean isPublicClient() {
        return getMongoEntity().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getMongoEntity().setPublicClient(flag);
        updateMongoEntity();
    }


    @Override
    public boolean isFrontchannelLogout() {
        return getMongoEntity().isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        getMongoEntity().setFrontchannelLogout(flag);
        updateMongoEntity();
    }

    @Override
    public boolean isFullScopeAllowed() {
        return getMongoEntity().isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getMongoEntity().setFullScopeAllowed(value);
        updateMongoEntity();

    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return getMongoEntity().getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getMongoEntity().setNotBefore(notBefore);
        updateMongoEntity();
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<MongoRoleEntity> roles = MongoModelUtils.getAllScopesOfClient(this, invocationContext);

        for (MongoRoleEntity role : roles) {
            if (realm.getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(session, realm, role, realm, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(session, realm, role, invocationContext));
            }
        }
        return result;
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        Set<RoleModel> allScopes = getScopeMappings();

        // Filter to retrieve just realm roles TODO: Maybe improve to avoid filter programmatically... Maybe have separate fields for realmRoles and appRoles on user?
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allScopes) {
            MongoRoleEntity roleEntity = ((RoleAdapter) role).getRole();

            if (realm.getId().equals(roleEntity.getRealmId())) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        getMongoStore().pushItemToList(this.getMongoEntity(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        getMongoStore().pullItemFromList(this.getMongoEntity(), "scopeIds", role.getId(), invocationContext);
    }

    @Override
    public String getProtocol() {
        return getMongoEntity().getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getMongoEntity().setProtocol(protocol);
        updateMongoEntity();

    }

    @Override
    public void setAttribute(String name, String value) {
        getMongoEntity().getAttributes().put(name, value);
        updateMongoEntity();

    }

    @Override
    public void removeAttribute(String name) {
        getMongoEntity().getAttributes().remove(name);
        updateMongoEntity();
    }

    @Override
    public String getAttribute(String name) {
        return getMongoEntity().getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(getMongoEntity().getAttributes());
        return copy;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        Set<ProtocolMapperModel> result = new HashSet<ProtocolMapperModel>();
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            ProtocolMapperModel mapping = new ProtocolMapperModel();
            mapping.setId(entity.getId());
            mapping.setName(entity.getName());
            mapping.setProtocol(entity.getProtocol());
            mapping.setProtocolMapper(entity.getProtocolMapper());
            mapping.setConsentRequired(entity.isConsentRequired());
            mapping.setConsentText(entity.getConsentText());
            Map<String, String> config = new HashMap<String, String>();
            if (entity.getConfig() != null) {
                config.putAll(entity.getConfig());
            }
            mapping.setConfig(config);
            result.add(mapping);
        }
        return result;
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new RuntimeException("protocol mapper name must be unique per protocol");
        }
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        String id = model.getId() != null ? model.getId() : KeycloakModelUtils.generateId();
        entity.setId(id);
        entity.setProtocol(model.getProtocol());
        entity.setName(model.getName());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setConfig(model.getConfig());
        entity.setConsentRequired(model.isConsentRequired());
        entity.setConsentText(model.getConsentText());
        getMongoEntity().getProtocolMappers().add(entity);
        updateMongoEntity();
        return entityToModel(entity);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getId().equals(mapping.getId())) {
                session.users().preRemove(this, mapping);

                getMongoEntity().getProtocolMappers().remove(entity);
                updateMongoEntity();
                break;
            }
        }

    }

    protected ProtocolMapperEntity getProtocolMapperyEntityById(String id) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }
    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }


    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(mapping.getId());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        entity.setConsentRequired(mapping.isConsentRequired());
        entity.setConsentText(mapping.getConsentText());
        if (entity.getConfig() != null) {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        } else {
            entity.setConfig(mapping.getConfig());
        }
        updateMongoEntity();

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        ProtocolMapperEntity entity = getProtocolMapperEntityByName(protocol, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected ProtocolMapperModel entityToModel(ProtocolMapperEntity entity) {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setProtocol(entity.getProtocol());
        mapping.setProtocolMapper(entity.getProtocolMapper());
        mapping.setConsentRequired(entity.isConsentRequired());
        mapping.setConsentText(entity.getConsentText());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }


    @Override
    public boolean isSurrogateAuthRequired() {
        return getMongoEntity().isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        getMongoEntity().setSurrogateAuthRequired(surrogateAuthRequired);
        updateMongoEntity();
    }

    @Override
    public String getManagementUrl() {
        return getMongoEntity().getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        getMongoEntity().setManagementUrl(url);
        updateMongoEntity();
    }

    @Override
    public void setRootUrl(String url) {
        getMongoEntity().setRootUrl(url);
        updateMongoEntity();
    }

    @Override
    public String getRootUrl() {
        return getMongoEntity().getRootUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        getMongoEntity().setBaseUrl(url);
        updateMongoEntity();
    }

    @Override
    public String getBaseUrl() {
        return getMongoEntity().getBaseUrl();
    }

    @Override
    public boolean isBearerOnly() {
        return getMongoEntity().isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getMongoEntity().setBearerOnly(only);
        updateMongoEntity();
    }

    @Override
    public boolean isConsentRequired() {
        return getMongoEntity().isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        getMongoEntity().setConsentRequired(consentRequired);
        updateMongoEntity();
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return getMongoEntity().isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        getMongoEntity().setServiceAccountsEnabled(serviceAccountsEnabled);
        updateMongoEntity();
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return getMongoEntity().isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        getMongoEntity().setDirectGrantsOnly(flag);
        updateMongoEntity();
    }

    @Override
    public RoleAdapter getRole(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("clientId").is(getId())
                .get();
        MongoRoleEntity role = getMongoStore().loadSingleEntity(MongoRoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(session, getRealm(), role, invocationContext);
        }
    }

    @Override
    public RoleAdapter addRole(String name) {
        return this.addRole(null, name);
    }

    @Override
    public RoleAdapter addRole(String id, String name) {
        MongoRoleEntity roleEntity = new MongoRoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setClientId(getId());

        getMongoStore().insertEntity(roleEntity, invocationContext);

        return new RoleAdapter(session, getRealm(), roleEntity, this, invocationContext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        session.users().preRemove(getRealm(), role);
        return getMongoStore().removeEntity(MongoRoleEntity.class, role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("clientId").is(getId())
                .get();
        List<MongoRoleEntity> roles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();
        for (MongoRoleEntity role : roles) {
            result.add(new RoleAdapter(session, getRealm(), role, this, invocationContext));
        }

        return result;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;
        Set<RoleModel> roles = getScopeMappings();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }

        roles = getRoles();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public Set<RoleModel> getClientScopeMappings(ClientModel client) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<MongoRoleEntity> roles = MongoModelUtils.getAllScopesOfClient(client, invocationContext);

        for (MongoRoleEntity role : roles) {
            if (getId().equals(role.getClientId())) {
                result.add(new RoleAdapter(session, getRealm(), role, this, invocationContext));
            }
        }
        return result;
    }

    @Override
    public List<String> getDefaultRoles() {
        return getMongoEntity().getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        getMongoStore().pushItemToList(getMongoEntity(), "defaultRoles", name, true, invocationContext);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        List<String> roleNames = new ArrayList<String>();
        for (String roleName : defaultRoles) {
            RoleModel role = getRole(roleName);
            if (role == null) {
                addRole(roleName);
            }

            roleNames.add(roleName);
        }

        getMongoEntity().setDefaultRoles(roleNames);
        updateMongoEntity();
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return getMongoEntity().getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        getMongoEntity().setNodeReRegistrationTimeout(timeout);
        updateMongoEntity();
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return getMongoEntity().getRegisteredNodes() == null ? Collections.<String, Integer>emptyMap() : Collections.unmodifiableMap(getMongoEntity().getRegisteredNodes());
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        MongoClientEntity entity = getMongoEntity();
        if (entity.getRegisteredNodes() == null) {
            entity.setRegisteredNodes(new HashMap<String, Integer>());
        }

        entity.getRegisteredNodes().put(nodeHost, registrationTime);
        updateMongoEntity();
    }

    @Override
    public void unregisterNode(String nodeHost) {
        MongoClientEntity entity = getMongoEntity();
        if (entity.getRegisteredNodes() == null) return;

        entity.getRegisteredNodes().remove(nodeHost);
        updateMongoEntity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
