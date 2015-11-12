package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanRealmCache implements RealmCache {

    protected static final Logger logger = Logger.getLogger(InfinispanRealmCache.class);

    protected final Cache<String, Object> cache;
    protected final ConcurrentHashMap<String, String> realmLookup;
    protected volatile boolean enabled = true;

    public InfinispanRealmCache(Cache<String, Object> cache, ConcurrentHashMap<String, String> realmLookup) {
        this.cache = cache;
        this.realmLookup = realmLookup;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        clear();
        this.enabled = enabled;
        clear();
    }

    @Override
    public CachedRealm getCachedRealm(String id) {
        if (!enabled) return null;
        return get(id, CachedRealm.class);
    }

    @Override
    public void invalidateCachedRealm(CachedRealm realm) {
        logger.tracev("Invalidating realm {0}", realm.getId());
        cache.remove(realm.getId());
        realmLookup.remove(realm.getName());
    }

    @Override
    public void invalidateCachedRealmById(String id) {
        CachedRealm cached = (CachedRealm) cache.remove(id);
        if (cached != null) realmLookup.remove(cached.getName());
    }

    @Override
    public void addCachedRealm(CachedRealm realm) {
        if (!enabled) return;
        logger.tracev("Adding realm {0}", realm.getId());
        cache.put(realm.getId(), realm);
        realmLookup.put(realm.getName(), realm.getId());
    }

    @Override
    public CachedRealm getCachedRealmByName(String name) {
        if (!enabled) return null;
        String id = realmLookup.get(name);
        return id != null ? getCachedRealm(id) : null;
    }

    @Override
    public CachedClient getApplication(String id) {
        if (!enabled) return null;
        return get(id, CachedClient.class);
    }

    @Override
    public void invalidateApplication(CachedClient app) {
        logger.tracev("Removing application {0}", app.getId());
        cache.remove(app.getId());
    }

    @Override
    public void addCachedClient(CachedClient app) {
        if (!enabled) return;
        logger.tracev("Adding application {0}", app.getId());
        cache.put(app.getId(), app);
    }

    @Override
    public void invalidateCachedApplicationById(String id) {
        logger.tracev("Removing application {0}", id);
        cache.remove(id);
    }

    @Override
    public CachedGroup getGroup(String id) {
        if (!enabled) return null;
        return get(id, CachedGroup.class);
    }

    @Override
    public void invalidateGroup(CachedGroup role) {
        logger.tracev("Removing group {0}", role.getId());
        cache.remove(role.getId());

    }

    @Override
    public void addCachedGroup(CachedGroup role) {
        if (!enabled) return;
        logger.tracev("Adding group {0}", role.getId());
        cache.put(role.getId(), role);

    }

    @Override
    public void invalidateCachedGroupById(String id) {
        logger.tracev("Removing group {0}", id);
        cache.remove(id);

    }

    @Override
    public void invalidateGroupById(String id) {
        logger.tracev("Removing group {0}", id);
        cache.remove(id);

    }

    @Override
    public CachedRole getRole(String id) {
        if (!enabled) return null;
        return get(id, CachedRole.class);
    }



    @Override
    public void invalidateRole(CachedRole role) {
        logger.tracev("Removing role {0}", role.getId());
        cache.remove(role.getId());
    }

    @Override
    public void invalidateRoleById(String id) {
        logger.tracev("Removing role {0}", id);
        cache.remove(id);
    }

    @Override
    public void addCachedRole(CachedRole role) {
        if (!enabled) return;
        logger.tracev("Adding role {0}", role.getId());
        cache.put(role.getId(), role);
    }

    @Override
    public void invalidateCachedRoleById(String id) {
        logger.tracev("Removing role {0}", id);
        cache.remove(id);
    }

    private <T> T get(String id, Class<T> type) {
        Object o = cache.get(id);
        return o != null && type.isInstance(o) ? type.cast(o) : null;
    }

}
