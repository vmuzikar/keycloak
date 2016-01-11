package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.timer.TimerProvider;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationProvidersResource {
    protected static final Logger logger = Logger.getLogger(UserFederationProvidersResource.class);

    protected RealmModel realm;

    protected  RealmAuth auth;
    
    protected AdminEventBuilder adminEvent;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public UserFederationProvidersResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent;
        
        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Automatically add "kerberos" to required realm credentials if it's supported by saved provider
     *
     * @param realm
     * @param model
     * @return true if kerberos credentials were added
     */
    public static boolean checkKerberosCredential(KeycloakSession session, RealmModel realm, UserFederationProviderModel model) {
        String allowKerberosCfg = model.getConfig().get(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION);
        if (Boolean.valueOf(allowKerberosCfg)) {
            CredentialHelper.setAlternativeCredential(session, CredentialRepresentation.KERBEROS, realm);
            return true;
        }

        return false;
    }

    /**
     * Get available provider factories
     *
     * Returns a list of available provider factories.
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserFederationProviderFactoryRepresentation> getProviders() {
        auth.requireView();
        List<UserFederationProviderFactoryRepresentation> providers = new LinkedList<UserFederationProviderFactoryRepresentation>();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory)factory).getConfigurationOptions());
            providers.add(rep);
        }
        return providers;
    }

    /**
     * Get factory with given id
     *
     * @return
     */
    @GET
    @NoCache
    @Path("providers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationProviderFactoryRepresentation getProvider(@PathParam("id") String id) {
        auth.requireView();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(UserFederationProvider.class)) {
            if (!factory.getId().equals(id)) {
                continue;
            }

            if (factory instanceof ConfiguredProvider) {

                UserFederationProviderFactoryDescription rep = new UserFederationProviderFactoryDescription();
                rep.setId(factory.getId());

                ConfiguredProvider cp = (ConfiguredProvider) factory;
                rep.setHelpText(cp.getHelpText());
                rep.setProperties(toConfigPropertyRepresentationList(cp.getConfigProperties()));

                return rep;
            }

            UserFederationProviderFactoryRepresentation rep = new UserFederationProviderFactoryRepresentation();
            rep.setId(factory.getId());
            rep.setOptions(((UserFederationProviderFactory) factory).getConfigurationOptions());

            return rep;
        }
        throw new NotFoundException("Could not find provider");
    }

    /**
     * Create a provider
     *
     * @param rep
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProviderInstance(UserFederationProviderRepresentation rep) {
        auth.requireManage();
        String displayName = rep.getDisplayName();
        if (displayName != null && displayName.trim().equals("")) {
            displayName = null;
        }
        UserFederationProviderModel model = realm.addUserFederationProvider(rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName,
                rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());
        new UsersSyncManager().refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), model, realm.getId());
        boolean kerberosCredsAdded = checkKerberosCredential(session, realm, model);
        if (kerberosCredsAdded) {
            logger.info("Added 'kerberos' to required realm credentials");
        }

        
        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(rep).success();

        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    /**
     * Get configured providers
     *
     * @return
     */
    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<UserFederationProviderRepresentation> getUserFederationInstances() {
        auth.requireManage();
        List<UserFederationProviderRepresentation> reps = new LinkedList<UserFederationProviderRepresentation>();
        for (UserFederationProviderModel model : realm.getUserFederationProviders()) {
            UserFederationProviderRepresentation rep = ModelToRepresentation.toRepresentation(model);
            reps.add(rep);
        }
        return reps;
    }

    @Path("instances/{id}")
    public UserFederationProviderResource getUserFederationInstance(@PathParam("id") String id) {
        this.auth.requireView();

        UserFederationProviderModel model = KeycloakModelUtils.findUserFederationProviderById(id, realm);
        if (model == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationProviderResource instanceResource = new UserFederationProviderResource(session, realm, this.auth, model, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(instanceResource);
        return instanceResource;
    }


    private ConfigPropertyRepresentation toConfigPropertyRepresentation(ProviderConfigProperty prop) {

        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
        propRep.setName(prop.getName());
        propRep.setLabel(prop.getLabel());
        propRep.setType(prop.getType());
        propRep.setDefaultValue(prop.getDefaultValue());
        propRep.setHelpText(prop.getHelpText());

        return propRep;
    }

    private List<ConfigPropertyRepresentation> toConfigPropertyRepresentationList(List<ProviderConfigProperty> props) {

        List<ConfigPropertyRepresentation> reps = new ArrayList<>(props.size());
        for(ProviderConfigProperty prop : props){
            reps.add(toConfigPropertyRepresentation(prop));
        }

        return reps;
    }


    public static class UserFederationProviderFactoryDescription extends UserFederationProviderFactoryRepresentation {

        protected String name;

        protected String helpText;

        protected List<ConfigPropertyRepresentation> properties;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHelpText() {
            return helpText;
        }

        public void setHelpText(String helpText) {
            this.helpText = helpText;
        }

        public List<ConfigPropertyRepresentation> getProperties() {
            return properties;
        }

        public void setProperties(List<ConfigPropertyRepresentation> properties) {
            this.properties = properties;
        }
    }
}
