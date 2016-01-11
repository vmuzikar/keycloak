package org.keycloak.testsuite;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProviderFactory implements UserFederationProviderFactory, ConfiguredProvider {

    private static final Logger logger = Logger.getLogger(DummyUserFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "dummy";

    private AtomicInteger fullSyncCounter = new AtomicInteger();
    private AtomicInteger changedSyncCounter = new AtomicInteger();

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return new DummyUserFederationProvider();
    }

    @Override
    public Set<String> getConfigurationOptions() {
        Set<String> list = new HashSet<String>();
        list.add("important.config");
        return list;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return new DummyUserFederationProvider();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        logger.info("syncAllUsers invoked");
        fullSyncCounter.incrementAndGet();
        return UserFederationSyncResult.empty();
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        logger.info("syncChangedUsers invoked");
        changedSyncCounter.incrementAndGet();
        return UserFederationSyncResult.empty();
    }

    public int getFullSyncCounter() {
        return fullSyncCounter.get();
    }

    public int getChangedSyncCounter() {
        return changedSyncCounter.get();
    }

    @Override
    public String getHelpText() {
        return "Dummy User Federation Provider Help Text";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        ProviderConfigProperty prop1 = new ProviderConfigProperty();
        prop1.setName("prop1");
        prop1.setLabel("Prop1");
        prop1.setDefaultValue("prop1Default");
        prop1.setHelpText("Prop1 HelpText");
        prop1.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty prop2 = new ProviderConfigProperty();
        prop2.setName("prop2");
        prop2.setLabel("Prop2");
        prop2.setDefaultValue("true");
        prop2.setHelpText("Prop2 HelpText");
        prop2.setType(ProviderConfigProperty.BOOLEAN_TYPE);

        return Arrays.asList(prop1, prop2);
    }
}
