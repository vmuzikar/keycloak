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

import org.keycloak.approvals.ApprovalManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalsResource {
    @Context
    protected KeycloakSession session;

    private RealmModel realm;

    public ApprovalsResource(RealmModel realm) {
        this.realm = realm;
    }

    @POST
    @Path("{id}")
    public Response approveRequest(final @PathParam("id") String requestId) {
        ApprovalManager approval = session.getProvider(ApprovalManager.class);

        if (!approval.approveRequest(requestId, realm)) {
            throw new NotFoundException("Approval request not found");
        }

        return Response.noContent().build();
    }
}
