package org.keycloak.connections.mongo;

import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.connections.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.connections.mongo.updater.MongoUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoConnectionFactoryProvider implements MongoConnectionProviderFactory, ServerInfoAwareProviderFactory {

    // TODO Make it dynamic
    private String[] entities = new String[]{
            "org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoUserEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoGroupEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoClientEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoOnlineUserSessionEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoOfflineUserSessionEntity",
            "org.keycloak.models.entities.IdentityProviderEntity",
            "org.keycloak.models.entities.ClientIdentityProviderMappingEntity",
            "org.keycloak.models.entities.RequiredCredentialEntity",
            "org.keycloak.models.entities.CredentialEntity",
            "org.keycloak.models.entities.FederatedIdentityEntity",
            "org.keycloak.models.entities.UserFederationProviderEntity",
            "org.keycloak.models.entities.UserFederationMapperEntity",
            "org.keycloak.models.entities.ProtocolMapperEntity",
            "org.keycloak.models.entities.IdentityProviderMapperEntity",
            "org.keycloak.models.entities.AuthenticationExecutionEntity",
            "org.keycloak.models.entities.AuthenticationFlowEntity",
            "org.keycloak.models.entities.AuthenticatorConfigEntity",
            "org.keycloak.models.entities.RequiredActionProviderEntity",
            "org.keycloak.models.entities.PersistentUserSessionEntity",
            "org.keycloak.models.entities.PersistentClientSessionEntity",
    };

    private static final Logger logger = Logger.getLogger(DefaultMongoConnectionFactoryProvider.class);

    private volatile MongoClient client;

    private MongoStore mongoStore;
    private DB db;
    protected Config.Scope config;
    
    private Map<String,String> operationalInfo;

    @Override
    public MongoConnectionProvider create(KeycloakSession session) {
        lazyInit(session);

        TransactionMongoStoreInvocationContext invocationContext = new TransactionMongoStoreInvocationContext(mongoStore);
        session.getTransaction().enlist(new MongoKeycloakTransaction(invocationContext));
        return new DefaultMongoConnectionProvider(db, mongoStore, invocationContext);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }


    private void lazyInit(KeycloakSession session) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        this.client = createMongoClient();

                        String dbName = config.get("db", "keycloak");
                        this.db = client.getDB(dbName);

                        String databaseSchema = config.get("databaseSchema");
                        if (databaseSchema != null) {
                            if (databaseSchema.equals("update")) {
                                MongoUpdaterProvider mongoUpdater = session.getProvider(MongoUpdaterProvider.class);

                                if (mongoUpdater == null) {
                                    throw new RuntimeException("Can't update database: Mongo updater provider not found");
                                }

                                mongoUpdater.update(session, db);
                            } else {
                                throw new RuntimeException("Invalid value for databaseSchema: " + databaseSchema);
                            }
                        }

                        this.mongoStore = new MongoStoreImpl(db, getManagedEntities());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private Class[] getManagedEntities() throws ClassNotFoundException {
       Class[] entityClasses = new Class[entities.length];
        for (int i = 0; i < entities.length; i++) {
            entityClasses[i] = Thread.currentThread().getContextClassLoader().loadClass(entities[i]);
        }
        return entityClasses;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String getId() {
        return "default";
    }

    /**
     * Override this method if you want more possibility to configure Mongo client. It can be also used to inject mongo client
     * from different source.
     *
     * This method can assume that "config" is already set and can use it.
     *
     * @return mongoClient instance, which will be shared for whole Keycloak
     *
     * @throws UnknownHostException
     */
    protected MongoClient createMongoClient() throws UnknownHostException {
        operationalInfo = new LinkedHashMap<>();
        String dbName = config.get("db", "keycloak");

        String uriString = config.get("uri");
        if (uriString != null) {
            MongoClientURI uri = new MongoClientURI(uriString);
            MongoClient client = new MongoClient(uri);

            StringBuilder hostsBuilder = new StringBuilder();
            for (int i=0 ; i<uri.getHosts().size() ; i++) {
                if (i!=0) {
                    hostsBuilder.append(", ");
                }
                hostsBuilder.append(uri.getHosts().get(i));
            }
            String hosts = hostsBuilder.toString();

            operationalInfo.put("mongoHosts", hosts);
            operationalInfo.put("mongoDatabaseName", dbName);
            operationalInfo.put("mongoUser", uri.getUsername());
            operationalInfo.put("mongoDriverVersion", client.getVersion());

            logger.debugv("Initialized mongo model. host(s): %s, db: %s", uri.getHosts(), dbName);
            return client;
        } else {
            String host = config.get("host", ServerAddress.defaultHost());
            int port = config.getInt("port", ServerAddress.defaultPort());

            String user = config.get("user");
            String password = config.get("password");

            MongoClientOptions clientOptions = getClientOptions();

            MongoClient client;
            if (user != null && password != null) {
                MongoCredential credential = MongoCredential.createMongoCRCredential(user, dbName, password.toCharArray());
                client = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential), clientOptions);
            } else {
                client = new MongoClient(new ServerAddress(host, port), clientOptions);
            }

            operationalInfo.put("mongoServerAddress", client.getAddress().toString());
            operationalInfo.put("mongoDatabaseName", dbName);
            operationalInfo.put("mongoUser", user);
            operationalInfo.put("mongoDriverVersion", client.getVersion());

            logger.debugv("Initialized mongo model. host: %s, port: %d, db: %s", host, port, dbName);
            return client;
        }
    }

    protected MongoClientOptions getClientOptions() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();
        checkIntOption("connectionsPerHost", builder);
        checkIntOption("threadsAllowedToBlockForConnectionMultiplier", builder);
        checkIntOption("maxWaitTime", builder);
        checkIntOption("connectTimeout", builder);
        checkIntOption("socketTimeout", builder);
        checkBooleanOption("socketKeepAlive", builder);
        checkBooleanOption("autoConnectRetry", builder);
        if (config.getLong("maxAutoConnectRetryTime") != null) {
            builder.maxAutoConnectRetryTime(config.getLong("maxAutoConnectRetryTime"));
        }
        if(config.getBoolean("ssl", false)) {
            builder.socketFactory(SSLSocketFactory.getDefault());
        }

        return builder.build();
    }

    protected void checkBooleanOption(String optionName, MongoClientOptions.Builder builder) {
        Boolean val = config.getBoolean(optionName);
        if (val != null) {
            try {
                Method m = MongoClientOptions.Builder.class.getMethod(optionName, boolean.class);
                m.invoke(builder, val);
            } catch (Exception e) {
                throw new IllegalStateException("Problem configuring boolean option " + optionName + " for mongo client. Ensure you used correct value true or false and if this option is supported by mongo driver", e);
            }
        }
    }

    protected void checkIntOption(String optionName, MongoClientOptions.Builder builder) {
        Integer val = config.getInt(optionName);
        if (val != null) {
            try {
                Method m = MongoClientOptions.Builder.class.getMethod(optionName, int.class);
                m.invoke(builder, val);
            } catch (Exception e) {
                throw new IllegalStateException("Problem configuring int option " + optionName + " for mongo client. Ensure you used correct value (number) and if this option is supported by mongo driver", e);
            }
        }
    }
    
    @Override
  	public Map<String,String> getOperationalInfo() {
  		return operationalInfo;
  	}

}
