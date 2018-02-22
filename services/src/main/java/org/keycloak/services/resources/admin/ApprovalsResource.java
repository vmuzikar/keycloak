/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.approvals.ApprovalManager;
import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ApprovalListenerConfigRepresentation;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalsResource {
    @Context
    protected KeycloakSession session;

    private RealmModel realm;
    private AdminEventBuilder adminEvent;
    protected ApprovalManager approval;

    protected ApprovalManager getApprovalsManager() {
        if (approval == null) {
            approval = session.getProvider(ApprovalManager.class);
        }
        return approval;
    }

    public ApprovalsResource(RealmModel realm, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.APPROVALS);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApprovalRequestRepresentation> getApprovals() {
        return getApprovalsManager()
                .getStore()
                .getRequestsForRealm(realm)
                .stream()
                .map(m -> ModelToRepresentation.toRepresentation(session, m))
                .collect(Collectors.toList());
    }

    @POST
    @Path("{id}")
    public Response approveRequest(final @Context UriInfo uriInfo, final @PathParam("id") String requestId) {
        ApprovalRequestRepresentation requestRep = getApprovalsManager().approveRequest(requestId, realm);
        if (requestRep == null) {
            throw new NotFoundException("Approval request not found");
        }

        adminEvent.operation(OperationType.APPROVED).resourcePath(uriInfo).representation(requestRep).success();
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response rejectRequest(final @Context UriInfo uriInfo, final @PathParam("id") String requestId) {
        ApprovalRequestRepresentation requestRep = getApprovalsManager().rejectRequest(requestId, realm);
        if (requestRep == null) {
            throw new NotFoundException("Approval request not found");
        }

        adminEvent.operation(OperationType.REJECTED).resourcePath(uriInfo).representation(requestRep).success();
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("listeners/{id}")
    public ApprovalListenerConfigRepresentation getListenerConfig(final @PathParam("id") String providerId) {
        ApprovalListenerConfigModel configModel = getApprovalsManager().getStore().createOrGetListenerConfig(providerId, realm);
        if (configModel == null) {
            throw new NotFoundException("Approval listener not found");
        }

        return ModelToRepresentation.toRepresentation(configModel, false);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("listeners/{id}")
    public Response updateListenerConfig(final @Context UriInfo uriInfo,
                                         final @PathParam("id") String providerId,
                                         final ApprovalListenerConfigRepresentation configRep) {
        ApprovalListenerConfigModel configModel = getApprovalsManager().getStore().createOrGetListenerConfig(providerId, realm);
        if (configModel == null) {
            throw new NotFoundException("Approval listener not found");
        }

        RepresentationToModel.updateApprovalListenerConfig(configModel, configRep);

        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(configRep).success();
        return Response.noContent().build();
    }
}
