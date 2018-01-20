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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

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
        this.adminEvent = adminEvent;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ApprovalRequestRepresentation> getApprovals() {
        return getApprovalsManager()
                .getStore()
                .getRequestsForRealm(realm)
                .stream()
                .map(m -> ModelToRepresentation.toRepresentation(m, approval))
                .collect(Collectors.toList());
    }

    @POST
    @Path("{id}")
    public Response approveRequest(final @PathParam("id") String requestId) {
        if (!getApprovalsManager().approveRequest(requestId, realm)) {
            throw new NotFoundException("Approval request not found");
        }

        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response rejectRequest(final @PathParam("id") String requestId) {
        if (!getApprovalsManager().rejectRequest(requestId, realm)) {
            throw new NotFoundException("Approval request not found");
        }

        return Response.noContent().build();
    }
}
