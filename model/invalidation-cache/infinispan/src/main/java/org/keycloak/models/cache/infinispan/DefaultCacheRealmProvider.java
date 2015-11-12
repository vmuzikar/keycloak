package org.keycloak.models.cache.infinispan;

import org.keycloak.migration.MigrationModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.*;
import org.keycloak.models.cache.entities.*;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultCacheRealmProvider implements CacheRealmProvider {
    protected RealmCache cache;
    protected KeycloakSession session;
    protected RealmProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Set<String> realmInvalidations = new HashSet<String>();
    protected Set<String> appInvalidations = new HashSet<String>();
    protected Set<String> roleInvalidations = new HashSet<String>();
    protected Set<String> groupInvalidations = new HashSet<String>();
    protected Map<String, RealmModel> managedRealms = new HashMap<String, RealmModel>();
    protected Map<String, ClientModel> managedApplications = new HashMap<String, ClientModel>();
    protected Map<String, RoleModel> managedRoles = new HashMap<String, RoleModel>();
    protected Map<String, GroupModel> managedGroups = new HashMap<String, GroupModel>();

    protected boolean clearAll;

    public DefaultCacheRealmProvider(RealmCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;

        session.getTransaction().enlistAfterCompletion(getTransaction());
    }

    @Override
    public MigrationModel getMigrationModel() {
        return getDelegate().getMigrationModel();
    }


    @Override
    public boolean isEnabled() {
        return cache.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        cache.setEnabled(enabled);
    }

    @Override
    public RealmProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(RealmProvider.class);
        return delegate;
    }

    @Override
    public void registerRealmInvalidation(String id) {
        realmInvalidations.add(id);
    }

    @Override
    public void registerApplicationInvalidation(String id) {
        appInvalidations.add(id);
    }

    @Override
    public void registerRoleInvalidation(String id) {
        roleInvalidations.add(id);
    }

    @Override
    public void registerGroupInvalidation(String id) {
        groupInvalidations.add(id);

    }

    protected void runInvalidations() {
        for (String id : realmInvalidations) {
            cache.invalidateCachedRealmById(id);
        }
        for (String id : roleInvalidations) {
            cache.invalidateRoleById(id);
        }
        for (String id : groupInvalidations) {
            cache.invalidateGroupById(id);
        }
        for (String id : appInvalidations) {
            cache.invalidateCachedApplicationById(id);
        }
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
                if (clearAll) {
                    cache.clear();
                }
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    @Override
    public RealmModel createRealm(String name) {
        RealmModel realm = getDelegate().createRealm(name);
        if (!cache.isEnabled()) return realm;
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmModel realm =  getDelegate().createRealm(id, name);
        if (!cache.isEnabled()) return realm;
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        if (!cache.isEnabled()) return getDelegate().getRealm(id);
        CachedRealm cached = cache.getCachedRealm(id);
        if (cached == null) {
            RealmModel model = getDelegate().getRealm(id);
            if (model == null) return null;
            if (realmInvalidations.contains(id)) return model;
            cached = new CachedRealm(cache, this, model);
            cache.addCachedRealm(cached);
        } else if (realmInvalidations.contains(id)) {
            return getDelegate().getRealm(id);
        } else if (managedRealms.containsKey(id)) {
            return managedRealms.get(id);
        }
        RealmAdapter adapter = new RealmAdapter(cached, this);
        managedRealms.put(id, adapter);
        return adapter;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        if (!cache.isEnabled()) return getDelegate().getRealmByName(name);
        CachedRealm cached = cache.getCachedRealmByName(name);
        if (cached == null) {
            RealmModel model = getDelegate().getRealmByName(name);
            if (model == null) return null;
            if (realmInvalidations.contains(model.getId())) return model;
            cached = new CachedRealm(cache, this, model);
            cache.addCachedRealm(cached);
        } else if (realmInvalidations.contains(cached.getId())) {
            return getDelegate().getRealmByName(name);
        } else if (managedRealms.containsKey(cached.getId())) {
            return managedRealms.get(cached.getId());
        }
        RealmAdapter adapter = new RealmAdapter(cached, this);
        managedRealms.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public List<RealmModel> getRealms() {
        // Retrieve realms from backend
        List<RealmModel> backendRealms = getDelegate().getRealms();

        // Return cache delegates to ensure cache invalidated during write operations
        List<RealmModel> cachedRealms = new LinkedList<RealmModel>();
        for (RealmModel realm : backendRealms) {
            RealmModel cached = getRealm(realm.getId());
            cachedRealms.add(cached);
        }
        return cachedRealms;
    }

    @Override
    public boolean removeRealm(String id) {
        if (!cache.isEnabled()) return getDelegate().removeRealm(id);
        cache.invalidateCachedRealmById(id);

        RealmModel realm = getDelegate().getRealm(id);
        Set<RoleModel> realmRoles = null;
        if (realm != null) {
            realmRoles = realm.getRoles();
        }

        boolean didIt = getDelegate().removeRealm(id);
        realmInvalidations.add(id);

        // TODO: Temporary workaround to invalidate cached realm roles
        if (didIt && realmRoles != null) {
            for (RoleModel role : realmRoles) {
                roleInvalidations.add(role.getId());
            }
        }

        return didIt;
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getRoleById(id, realm);
        CachedRole cached = cache.getRole(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            RoleModel model = getDelegate().getRoleById(id, realm);
            if (model == null) return null;
            if (roleInvalidations.contains(id)) return model;
            if (model.getContainer() instanceof ClientModel) {
                cached = new CachedClientRole(((ClientModel) model.getContainer()).getId(), model, realm);
            } else {
                cached = new CachedRealmRole(model, realm);
            }
            cache.addCachedRole(cached);

        } else if (roleInvalidations.contains(id)) {
            return getDelegate().getRoleById(id, realm);
        } else if (managedRoles.containsKey(id)) {
            return managedRoles.get(id);
        }
        RoleAdapter adapter = new RoleAdapter(cached, cache, this, realm);
        managedRoles.put(id, adapter);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getGroupById(id, realm);
        CachedGroup cached = cache.getGroup(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            GroupModel model = getDelegate().getGroupById(id, realm);
            if (model == null) return null;
            if (groupInvalidations.contains(id)) return model;
            cached = new CachedGroup(realm, model);
            cache.addCachedGroup(cached);

        } else if (groupInvalidations.contains(id)) {
            return getDelegate().getGroupById(id, realm);
        } else if (managedGroups.containsKey(id)) {
            return managedGroups.get(id);
        }
        GroupAdapter adapter = new GroupAdapter(cached, this, session, realm);
        managedGroups.put(id, adapter);
        return adapter;
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getClientById(id, realm);
        CachedClient cached = cache.getApplication(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            ClientModel model = getDelegate().getClientById(id, realm);
            if (model == null) return null;
            if (appInvalidations.contains(id)) return model;
            cached = new CachedClient(cache, getDelegate(), realm, model);
            cache.addCachedClient(cached);
        } else if (appInvalidations.contains(id)) {
            return getDelegate().getClientById(id, realm);
        } else if (managedApplications.containsKey(id)) {
            return managedApplications.get(id);
        }
        ClientAdapter adapter = new ClientAdapter(realm, cached, this, cache);
        managedApplications.put(id, adapter);
        return adapter;
    }

}
