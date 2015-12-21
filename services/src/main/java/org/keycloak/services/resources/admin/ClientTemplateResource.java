package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.TRUE;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateResource {
    protected static final Logger logger = Logger.getLogger(ClientTemplateResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    protected ClientTemplateModel template;
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public ClientTemplateResource(RealmModel realm, RealmAuth auth, ClientTemplateModel template, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.template = template;
        this.session = session;
        this.adminEvent = adminEvent;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        ProtocolMappersResource mappers = new ProtocolMappersResource(template, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        return mappers;
    }

    /**
     * Base path for managing the scope mappings for the client
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, template, session, adminEvent);
    }

    /**
     * Update the client template
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ClientTemplateRepresentation rep) {
        auth.requireManage();

        try {
            RepresentationToModel.updateClientTemplate(rep, template);
            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client Template " + rep.getName() + " already exists");
        }
    }


    /**
     * Get representation of the client template
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientTemplateRepresentation getClient() {
        auth.requireView();
        return ModelToRepresentation.toRepresentation(template);
    }

    /**
     * Delete the client template
     *
     */
    @DELETE
    @NoCache
    public void deleteClientTemplate() {
        auth.requireManage();
        realm.removeClientTemplate(template.getId());
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }



}
