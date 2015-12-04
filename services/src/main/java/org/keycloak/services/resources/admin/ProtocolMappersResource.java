package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.ErrorResponse;

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

import java.util.LinkedList;
import java.util.List;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMappersResource {
    protected static final Logger logger = Logger.getLogger(ProtocolMappersResource.class);
    
    protected ClientModel client;

    protected RealmAuth auth;
    
    protected AdminEventBuilder adminEvent;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public ProtocolMappersResource(ClientModel client, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.client = client;
        this.adminEvent = adminEvent;

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Get mappers by name for a specific protocol
     *
     * @param protocol
     * @return
     */
    @GET
    @NoCache
    @Path("protocol/{protocol}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol) {
        auth.requireView();
        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            if (mapper.getProtocol().equals(protocol)) mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    /**
     * Create a mapper
     *
     * @param rep
     */
    @Path("models")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMapper(ProtocolMapperRepresentation rep) {
        auth.requireManage();

        ProtocolMapperModel model = null;
        try {
            model = RepresentationToModel.toModel(rep);
            model = client.addProtocolMapper(model);
            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, model.getId()).representation(rep).success();

        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Protocol mapper exists with same name");
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }
    /**
     * Create multiple mappers
     *
     */
    @Path("add-models")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void createMapper(List<ProtocolMapperRepresentation> reps) {
        auth.requireManage();
        ProtocolMapperModel model = null;
        for (ProtocolMapperRepresentation rep : reps) {
            model = RepresentationToModel.toModel(rep);
            model = client.addProtocolMapper(model);
        }
        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(reps).success();
    }

    /**
     * Get mappers
     *
     * @return
     */
    @GET
    @NoCache
    @Path("models")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProtocolMapperRepresentation> getMappers() {
        auth.requireView();
        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    /**
     * Get mapper by id
     *
     * @param id Mapper id
     * @return
     */
    @GET
    @NoCache
    @Path("models/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProtocolMapperRepresentation getMapperById(@PathParam("id") String id) {
        auth.requireView();
        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Update the mapper
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("models/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, ProtocolMapperRepresentation rep) {
        auth.requireManage();
        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);
        client.updateProtocolMapper(model);
        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();
    }

    /**
     * Delete the mapper
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("models/{id}")
    public void delete(@PathParam("id") String id) {
        auth.requireManage();
        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        client.removeProtocolMapper(model);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();

    }

}
