package org.keycloak.connections.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultInfinispanConnectionProviderFactory.class);

    private Config.Scope config;

    private EmbeddedCacheManager cacheManager;

    private boolean containerManaged;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        lazyInit();

        return new DefaultInfinispanConnectionProvider(cacheManager);
    }

    @Override
    public void close() {
        if (cacheManager != null && !containerManaged) {
            cacheManager.stop();
        }
        cacheManager = null;
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    private void lazyInit() {
        if (cacheManager == null) {
            synchronized (this) {
                if (cacheManager == null) {
                    String cacheContainer = config.get("cacheContainer");
                    if (cacheContainer != null) {
                        initContainerManaged(cacheContainer);
                    } else {
                        initEmbedded();
                    }

                    // Backwards compatibility
                    if (cacheManager.getCacheConfiguration(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) == null) {
                        logger.debugf("No configuration provided for '%s' cache. Using '%s' configuration as template",
                                InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, InfinispanConnectionProvider.SESSION_CACHE_NAME);

                        Configuration sessionCacheConfig = cacheManager.getCacheConfiguration(InfinispanConnectionProvider.SESSION_CACHE_NAME);
                        if (sessionCacheConfig != null) {
                            ConfigurationBuilder confBuilder = new ConfigurationBuilder().read(sessionCacheConfig);
                            Configuration offlineSessionConfig = confBuilder.build();
                            cacheManager.defineConfiguration(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, offlineSessionConfig);
                        }
                    }
                }
            }
        }
    }

    private void initContainerManaged(String cacheContainerLookup) {
        try {
            cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);
            containerManaged = true;

            logger.debugv("Using container managed Infinispan cache container, lookup={1}", cacheContainerLookup);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve cache container", e);
        }
    }

    private void initEmbedded() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = config.getBoolean("clustered", false);
        boolean async = config.getBoolean("async", true);
        boolean allowDuplicateJMXDomains = config.getBoolean("allowDuplicateJMXDomains", true);

        if (clustered) {
            gcb.transport().defaultTransport();
        }
        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        cacheManager = new DefaultCacheManager(gcb.build());
        containerManaged = false;

        logger.debug("Started embedded Infinispan cache container");

        ConfigurationBuilder invalidationConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            invalidationConfigBuilder.clustering().cacheMode(async ? CacheMode.INVALIDATION_ASYNC : CacheMode.INVALIDATION_SYNC);
        }
        Configuration invalidationCacheConfiguration = invalidationConfigBuilder.build();

        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME, invalidationCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_CACHE_NAME, invalidationCacheConfiguration);

        ConfigurationBuilder sessionConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            String sessionsMode = config.get("sessionsMode", "distributed");
            if (sessionsMode.equalsIgnoreCase("replicated")) {
                sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
            } else if (sessionsMode.equalsIgnoreCase("distributed")) {
                sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);
            } else {
                throw new RuntimeException("Invalid value for sessionsMode");
            }

            sessionConfigBuilder.clustering().hash()
                    .numOwners(config.getInt("sessionsOwners", 2))
                    .numSegments(config.getInt("sessionsSegments", 60)).build();
        }

        Configuration sessionCacheConfiguration = sessionConfigBuilder.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.SESSION_CACHE_NAME, sessionCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME, sessionCacheConfiguration);
    }

}
