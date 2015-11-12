package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @author Bill Burke
 */
public class AuthenticationManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    @Context
    private UriInfo uriInfo;

    private static Logger logger = Logger.getLogger(AuthenticationManagementResource.class);

    public AuthenticationManagementResource(RealmModel realm, KeycloakSession session, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.auth.init(RealmAuth.Resource.REALM);
        this.adminEvent = adminEvent;
    }

    public static class AuthenticationExecutionRepresentation {
        protected String id;
        protected String requirement;
        protected String displayName;
        protected List<String> requirementChoices;
        protected Boolean configurable;
        protected Boolean authenticationFlow;
        protected String providerId;
        protected String authenticationConfig;
        protected String flowId;
        protected int level;
        protected int index;

        public String getId() {
            return id;
        }

        public void setId(String execution) {
            this.id = execution;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }

        public List<String> getRequirementChoices() {
            return requirementChoices;
        }

        public void setRequirementChoices(List<String> requirementChoices) {
            this.requirementChoices = requirementChoices;
        }

        public Boolean getConfigurable() {
            return configurable;
        }

        public void setConfigurable(Boolean configurable) {
            this.configurable = configurable;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getAuthenticationConfig() {
            return authenticationConfig;
        }

        public void setAuthenticationConfig(String authenticationConfig) {
            this.authenticationConfig = authenticationConfig;
        }

        public Boolean getAuthenticationFlow() {
            return authenticationFlow;
        }

        public void setAuthenticationFlow(Boolean authenticationFlow) {
            this.authenticationFlow = authenticationFlow;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getFlowId() {
            return flowId;
        }

        public void setFlowId(String flowId) {
            this.flowId = flowId;
        }
    }

    /**
     * Get form providers
     *
     * Returns a list of form providers.
     */
    @Path("/form-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getFormProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(FormAuthenticator.class);
        return buildProviderMetadata(factories);
    }

    /**
     * Get authenticator providers
     *
     * Returns a list of authenticator providers.
     */
    @Path("/authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getAuthenticatorProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(Authenticator.class);
        return buildProviderMetadata(factories);
    }

    /**
     * Get client authenticator providers
     *
     * Returns a list of client authenticator providers.
     */
    @Path("/client-authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getClientAuthenticatorProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(ClientAuthenticator.class);
        return buildProviderMetadata(factories);
    }

    public List<Map<String, Object>> buildProviderMetadata(List<ProviderFactory> factories) {
        List<Map<String, Object>> providers = new LinkedList<>();
        for (ProviderFactory factory : factories) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", factory.getId());
            ConfigurableAuthenticatorFactory configured = (ConfigurableAuthenticatorFactory)factory;
            data.put("description", configured.getHelpText());
            data.put("displayName", configured.getDisplayType());

            providers.add(data);
        }
        return providers;
    }

    /**
     * Get form action providers
     *
     * Returns a list of form action providers.
     */
    @Path("/form-action-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getFormActionProviders() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(FormAction.class);
        return buildProviderMetadata(factories);
    }


    /**
     * Get authentication flows
     *
     * Returns a list of authentication flows.
     */
    @Path("/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthenticationFlowModel> getFlows() {
        this.auth.requireView();
        List<AuthenticationFlowModel> flows = new LinkedList<>();
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            if (flow.isTopLevel()) {
                flows.add(flow);
            }
        }
        return flows;
    }

    /**
     * Create a new authentication flow
     *
     * @param model Authentication flow model
     * @return
     */
    @Path("/flows")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFlow(AuthenticationFlowModel model) {
        this.auth.requireManage();
        
        if (model.getAlias() == null || model.getAlias().isEmpty()) {
            return ErrorResponse.exists("Failed to create flow with empty alias name");
        }

        if (realm.getFlowByAlias(model.getAlias()) != null) {
            return ErrorResponse.exists("Flow " + model.getAlias() + " already exists");
        }

        realm.addAuthenticationFlow(model);
        return Response.status(201).build();

    }

    /**
     * Get authentication flow for id
     *
     * @param id Flow id
     * @return
     */
    @Path("/flows/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowModel getFlow(@PathParam("id") String id) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        return flow;
    }

    /**
     * Delete an authentication flow
     *
     * @param id Flow id
     */
    @Path("/flows/{id}")
    @DELETE
    @NoCache
    public void deleteFlow(@PathParam("id") String id) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        if (flow.isBuiltIn()) {
            throw new BadRequestException("Can't delete built in flow");
        }
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(id);
        for (AuthenticationExecutionModel execution : executions) {
        	if(execution.getFlowId() != null) {
        		AuthenticationFlowModel nonTopLevelFlow = realm.getAuthenticationFlowById(execution.getFlowId());
        		realm.removeAuthenticationFlow(nonTopLevelFlow);
        	}
        	realm.removeAuthenticatorExecution(execution);
        }
        realm.removeAuthenticationFlow(flow);
    }

    /**
     * Copy existing authentication flow under a new name
     *
     * The new name is given as 'newName' attribute of the passed JSON object
     *
     * @param flowAlias Name of the existing authentication flow
     * @param data JSON containing 'newName' attribute
     */
    @Path("/flows/{flowAlias}/copy")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response copy(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        String newName = data.get("newName");
        if (realm.getFlowByAlias(newName) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        AuthenticationFlowModel copy = new AuthenticationFlowModel();
        copy.setAlias(newName);
        copy.setDescription(flow.getDescription());
        copy.setProviderId(flow.getProviderId());
        copy.setBuiltIn(false);
        copy.setTopLevel(flow.isTopLevel());
        copy = realm.addAuthenticationFlow(copy);
        copy(newName, flow, copy);

        return Response.status(201).build();

    }

    protected void copy(String newName, AuthenticationFlowModel from, AuthenticationFlowModel to) {
        for (AuthenticationExecutionModel execution : realm.getAuthenticationExecutions(from.getId())) {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                AuthenticationFlowModel copy = new AuthenticationFlowModel();
                copy.setAlias(newName + " " + subFlow.getAlias());
                copy.setDescription(subFlow.getDescription());
                copy.setProviderId(subFlow.getProviderId());
                copy.setBuiltIn(false);
                copy.setTopLevel(false);
                copy = realm.addAuthenticationFlow(copy);
                execution.setFlowId(copy.getId());
                copy(newName, subFlow, copy);
            }
            execution.setId(null);
            execution.setParentFlow(to.getId());
            realm.addAuthenticatorExecution(execution);
        }
    }

    /**
     * Add new flow with new execution to existing flow
     *
     * @param flowAlias Alias of parent authentication flow
     * @param data New authentication flow / execution JSON data containing 'alias', 'type', 'provider', and 'description' attributes
     */
    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void addExecutionFlow(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exists");
        }
        String alias = data.get("alias");
        String type = data.get("type");
        String provider = data.get("provider");
        String description = data.get("description");


        AuthenticationFlowModel newFlow = realm.getFlowByAlias(alias);
        if (newFlow != null) {
            throw new BadRequestException("New flow alias name already exists");
        }
        newFlow = new AuthenticationFlowModel();
        newFlow.setAlias(alias);
        newFlow.setDescription(description);
        newFlow.setProviderId(type);
        newFlow = realm.addAuthenticationFlow(newFlow);
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setFlowId(newFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(true);
        execution.setAuthenticator(provider);
        execution.setPriority(getNextPriority(parentFlow));

        realm.addAuthenticatorExecution(execution);
    }

    private int getNextPriority(AuthenticationFlowModel parentFlow) {
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        return executions.isEmpty() ? 0 : executions.get(executions.size() - 1).getPriority() + 1;
    }

    /**
     * Add new authentication execution to a flow
     *
     * @param flowAlias Alias of parent flow
     * @param data New execution JSON data containing 'provider' attribute
     */
    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void addExecution(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        this.auth.requireManage();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exists");
        }
        String provider = data.get("provider");

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(provider);
        execution.setPriority(getNextPriority(parentFlow));

        realm.addAuthenticatorExecution(execution);
    }

    /**
     * Get authentication executions for a flow
     *
     * @param flowAlias Flow alias
     */
    @Path("/flows/{flowAlias}/executions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutions(@PathParam("flowAlias") String flowAlias) {
        this.auth.requireView();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        List<AuthenticationExecutionRepresentation> result = new LinkedList<>();

        int level = 0;

        recurseExecutions(flow, result, level);
        return Response.ok(result).build();
    }

    public void recurseExecutions(AuthenticationFlowModel flow, List<AuthenticationExecutionRepresentation> result, int level) {
        int index = 0;
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(flow.getId());
        for (AuthenticationExecutionModel execution : executions) {
            AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
            rep.setLevel(level);
            rep.setIndex(index++);
            rep.setRequirementChoices(new LinkedList<String>());
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel flowRef = realm.getAuthenticationFlowById(execution.getFlowId());
                if (AuthenticationFlow.BASIC_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                } else if (AuthenticationFlow.FORM_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.setProviderId(execution.getAuthenticator());
                    rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                } else if (AuthenticationFlow.CLIENT_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                }
                rep.setDisplayName(flowRef.getAlias());
                rep.setConfigurable(false);
                rep.setId(execution.getId());
                rep.setAuthenticationFlow(execution.isAuthenticatorFlow());
                rep.setRequirement(execution.getRequirement().name());
                rep.setFlowId(execution.getFlowId());
                result.add(rep);
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                recurseExecutions(subFlow, result, level + 1);
            } else {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
                rep.setDisplayName(factory.getDisplayType());
                rep.setConfigurable(factory.isConfigurable());
                for (AuthenticationExecutionModel.Requirement choice : factory.getRequirementChoices()) {
                    rep.getRequirementChoices().add(choice.name());
                }
                rep.setId(execution.getId());
                rep.setRequirement(execution.getRequirement().name());
                rep.setProviderId(execution.getAuthenticator());
                rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                result.add(rep);
            }
        }
    }

    /**
     * Update authentication executions of a flow
     *
     * @param flowAlias Flow alias
     * @param rep
     */
    @Path("/flows/{flowAlias}/executions")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateExecutions(@PathParam("flowAlias") String flowAlias, AuthenticationExecutionRepresentation rep) {
        this.auth.requireManage();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            throw new NotFoundException("flow not found");
        }

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(rep.getId());
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        if (!model.getRequirement().name().equals(rep.getRequirement())) {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            realm.updateAuthenticatorExecution(model);
        }
    }

    /**
     * Add new authentication execution
     *
     * @param model JSON model describing authentication execution
     */
    @Path("/executions")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addExecution(AuthenticationExecutionModel model) {
        this.auth.requireManage();
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        model.setPriority(getNextPriority(parentFlow));
        model = realm.addAuthenticatorExecution(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    public AuthenticationFlowModel getParentFlow(AuthenticationExecutionModel model) {
        if (model.getParentFlow() == null) {
            throw new BadRequestException("parent flow not set on new execution");
        }
        AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(model.getParentFlow());
        if (parentFlow == null) {
            throw new BadRequestException("execution parent flow does not exist");

        }
        return parentFlow;
    }

    /**
     * Raise execution's priority
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}/raise-priority")
    @POST
    @NoCache
    public void raisePriority(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        AuthenticationExecutionModel previous = null;
        for (AuthenticationExecutionModel exe : executions) {
            if (exe.getId().equals(model.getId())) {
                break;
            }
            previous = exe;

        }
        if (previous == null) return;
        int tmp = previous.getPriority();
        previous.setPriority(model.getPriority());
        realm.updateAuthenticatorExecution(previous);
        model.setPriority(tmp);
        realm.updateAuthenticatorExecution(model);
    }

    public List<AuthenticationExecutionModel> getSortedExecutions(AuthenticationFlowModel parentFlow) {
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(parentFlow.getId());
        Collections.sort(executions, AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
        return executions;
    }

    /**
     * Lower execution's priority
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}/lower-priority")
    @POST
    @NoCache
    public void lowerPriority(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }
        List<AuthenticationExecutionModel> executions = getSortedExecutions(parentFlow);
        int i = 0;
        for (i = 0; i < executions.size(); i++) {
            if (executions.get(i).getId().equals(model.getId())) {
                break;
            }
        }
        if (i + 1 >= executions.size()) return;
        AuthenticationExecutionModel next = executions.get(i + 1);
        int tmp = model.getPriority();
        model.setPriority(next.getPriority());
        realm.updateAuthenticatorExecution(model);
        next.setPriority(tmp);
        realm.updateAuthenticatorExecution(next);
    }


    /**
     * Delete execution
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}")
    @DELETE
    @NoCache
    public void removeExecution(@PathParam("executionId") String execution) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to remove execution from a built in flow");
        }
        
        if(model.getFlowId() != null) {
        	AuthenticationFlowModel nonTopLevelFlow = realm.getAuthenticationFlowById(model.getFlowId());
        	realm.removeAuthenticationFlow(nonTopLevelFlow);
        }
		
        realm.removeAuthenticatorExecution(model);
    }


    /**
     * Update execution with new configuration
     *
     * @param execution Execution id
     * @param config JSON with new configuration
     * @return
     */
    @Path("/executions/{executionId}/config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newExecutionConfig(@PathParam("executionId") String execution, AuthenticatorConfigModel config) {
        this.auth.requireManage();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransaction().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        config = realm.addAuthenticatorConfig(config);
        model.setAuthenticatorConfig(config.getId());
        realm.updateAuthenticatorExecution(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get execution's configuration
     *
     * @param execution Execution id
     * @param id Configuration id
     */
    @Path("/executions/{executionId}/config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigModel getAuthenticatorConfig(@PathParam("executionId") String execution,@PathParam("id") String id) {
        this.auth.requireView();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return config;
    }


    public static class RequiredActionProviderRepresentation {
        private String alias;
        private String name;
        private boolean enabled;
        private boolean defaultAction;
        private Map<String, String> config = new HashMap<String, String>();

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDefaultAction() {
            return defaultAction;
        }

        public void setDefaultAction(boolean defaultAction) {
            this.defaultAction = defaultAction;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public void setConfig(Map<String, String> config) {
            this.config = config;
        }
    }

    /**
     * Get unregistered required actions
     *
     * Returns a list of unregistered required actions.
     */
    @Path("unregistered-required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<Map<String, String>> getUnregisteredRequiredActions() {
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class);
        List<Map<String, String>> unregisteredList = new LinkedList<>();
        for (ProviderFactory factory : factories) {
            RequiredActionFactory requiredActionFactory = (RequiredActionFactory) factory;
            boolean found = false;
            for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
                if (model.getProviderId().equals(factory.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, String> data = new HashMap<>();
                data.put("name", requiredActionFactory.getDisplayText());
                data.put("providerId", requiredActionFactory.getId());
                unregisteredList.add(data);
            }

        }
        return unregisteredList;
    }

    /**
     * Register a new required actions
     *
     * @param data JSON containing 'providerId', and 'name' attributes.
     */
    @Path("register-required-action")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void registereRequiredAction(Map<String, String> data) {
        String providerId = data.get("providerId");
        String name = data.get("name");
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(providerId);
        requiredAction.setName(name);
        requiredAction.setProviderId(providerId);
        requiredAction.setDefaultAction(false);
        requiredAction.setEnabled(true);
        realm.addRequiredActionProvider(requiredAction);
    }


    /**
     * Get required actions
     *
     * Returns a list of required actions.
     */
    @Path("required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RequiredActionProviderRepresentation> getRequiredActions() {
        List<RequiredActionProviderRepresentation> list = new LinkedList<>();
        for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
            RequiredActionProviderRepresentation rep = toRepresentation(model);
            list.add(rep);
        }
        return list;
    }

    public static RequiredActionProviderRepresentation toRepresentation(RequiredActionProviderModel model) {
        RequiredActionProviderRepresentation rep = new RequiredActionProviderRepresentation();
        rep.setAlias(model.getAlias());
        rep.setName(model.getName());
        rep.setDefaultAction(model.isDefaultAction());
        rep.setEnabled(model.isEnabled());
        rep.setConfig(model.getConfig());
        return rep;
    }

    /**
     * Get required action for alias
     * @param alias Alias of required action
     */
    @Path("required-actions/{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public RequiredActionProviderRepresentation getRequiredAction(@PathParam("alias") String alias) {
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }
        return toRepresentation(model);
    }


    /**
     * Update required action
     *
     * @param alias Alias of required action
     * @param rep JSON describing new state of required action
     */
    @Path("required-actions/{alias}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRequiredAction(@PathParam("alias") String alias, RequiredActionProviderRepresentation rep) {
        this.auth.requireManage();
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }
        RequiredActionProviderModel update = new RequiredActionProviderModel();
        update.setId(model.getId());
        update.setName(rep.getName());
        update.setAlias(rep.getAlias());
        update.setProviderId(model.getProviderId());
        update.setDefaultAction(rep.isDefaultAction());
        update.setEnabled(rep.isEnabled());
        update.setConfig(rep.getConfig());
        realm.updateRequiredActionProvider(update);
    }

    /**
     * Delete required action
     * @param alias Alias of required action
     */
    @Path("required-actions/{alias}")
    @DELETE
    public void updateRequiredAction(@PathParam("alias") String alias) {
        this.auth.requireManage();
        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }
        realm.removeRequiredActionProvider(model);
    }

    public class AuthenticatorConfigDescription {
        protected String name;
        protected String providerId;
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

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
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


    /**
     * Get authenticator provider's configuration description
     */
    @Path("config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigDescription getAuthenticatorConfigDescription(@PathParam("providerId") String providerId) {
        this.auth.requireView();
        ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find authenticator provider");
        }
        AuthenticatorConfigDescription rep = new AuthenticatorConfigDescription();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayType());
        rep.setHelpText(factory.getHelpText());
        rep.setProperties(new LinkedList<ConfigPropertyRepresentation>());
        List<ProviderConfigProperty> configProperties = factory.getConfigProperties();
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    private ConfigPropertyRepresentation getConfigPropertyRep(ProviderConfigProperty prop) {
        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
        propRep.setName(prop.getName());
        propRep.setLabel(prop.getLabel());
        propRep.setType(prop.getType());
        propRep.setDefaultValue(prop.getDefaultValue());
        propRep.setHelpText(prop.getHelpText());
        return propRep;
    }

    /**
     *  Get configuration descriptions for all clients
     */
    @Path("per-client-config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, List<ConfigPropertyRepresentation>> getPerClientConfigDescription() {
        this.auth.requireView();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(ClientAuthenticator.class);

        Map<String, List<ConfigPropertyRepresentation>> toReturn = new HashMap<>();
        for (ProviderFactory clientAuthenticatorFactory : factories) {
            String providerId = clientAuthenticatorFactory.getId();
            ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
            ClientAuthenticatorFactory clientAuthFactory = (ClientAuthenticatorFactory) factory;
            List<ProviderConfigProperty> perClientConfigProps = clientAuthFactory.getConfigPropertiesPerClient();
            List<ConfigPropertyRepresentation> result = new LinkedList<>();
            for (ProviderConfigProperty prop : perClientConfigProps) {
                ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
                result.add(propRep);
            }

            toReturn.put(providerId, result);
        }

        return toReturn;
    }

    /**
     * Create new authenticator configuration
     * @param config JSON describing new authenticator configuration
     */
    @Path("config")
    @POST
    @NoCache
    public Response createAuthenticatorConfig(AuthenticatorConfigModel config) {
        this.auth.requireManage();
        config = realm.addAuthenticatorConfig(config);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get authenticator configuration
     * @param id Configuration id
     */
    @Path("config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigModel getAuthenticatorConfig(@PathParam("id") String id) {
        this.auth.requireView();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return config;
    }

    /**
     * Delete authenticator configuration
     * @param id Configuration id
     */
    @Path("config/{id}")
    @DELETE
    @NoCache
    public void removeAuthenticatorConfig(@PathParam("id") String id) {
        this.auth.requireManage();
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        List<AuthenticationFlowModel> flows = new LinkedList<>();
        for (AuthenticationFlowModel flow : realm.getAuthenticationFlows()) {
            for (AuthenticationExecutionModel exe : realm.getAuthenticationExecutions(flow.getId())) {
                if (id.equals(exe.getAuthenticatorConfig())) {
                    exe.setAuthenticatorConfig(null);
                    realm.updateAuthenticatorExecution(exe);
                }
            }
        }

        realm.removeAuthenticatorConfig(config);
    }

    /**
     * Update authenticator configuration
     * @param id Configuration id
     * @param config JSON describing new state of authenticator configuration
     */
    @Path("config/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateAuthenticatorConfig(@PathParam("id") String id, AuthenticatorConfigModel config) {
        this.auth.requireManage();
        AuthenticatorConfigModel exists = realm.getAuthenticatorConfigById(id);
        if (exists == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        exists.setAlias(config.getAlias());
        exists.setConfig(config.getConfig());
        realm.updateAuthenticatorConfig(exists);
    }




}