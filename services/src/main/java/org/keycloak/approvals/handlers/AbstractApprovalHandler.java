/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.approvals.handlers;

import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalHandler;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractApprovalHandler implements ApprovalHandler {
    protected KeycloakSession session;

    public AbstractApprovalHandler(KeycloakSession session) {
        this.session = session;
    }

    protected ApprovalRequestRepresentation contextToRepresentation(ApprovalContext context) {
        return contextToRepresentation(context, null);
    }

    protected ApprovalRequestRepresentation contextToRepresentation(ApprovalContext context, String description) {
        ApprovalRequestRepresentation rep = new ApprovalRequestRepresentation();

        rep.setDescription(description);
        rep.setAction(context.getAction());

        rep.setAttributes(new HashMap<>());
        try {
            if (context.getRepresentation() != null) {
                rep.getAttributes().put(ApprovalContext.REPRESENTATION_ATTR, JsonSerialization.writeValueAsString(context.getRepresentation()));
                if (description == null) {
                    rep.setDescription(JsonSerialization.writeValueAsPrettyString(context.getRepresentation()));
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return rep;
    }

    @Override
    public void close() {

    }
}
