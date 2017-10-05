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

package org.keycloak.approvals.handlers;

import org.jboss.logging.Logger;
import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalHandler;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalRequestStore;
import org.keycloak.approvals.store.ApprovalStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractApprovalHandler implements ApprovalHandler {
    private final Logger log = Logger.getLogger(this.getClass());

    protected KeycloakSession session;
    protected ApprovalRequestStore requestStore;

    public void setKeycloakSession(KeycloakSession session) {
        this.session = session;
        this.requestStore = session.getProvider(ApprovalStoreProvider.class).getRequestStore();
    }

    @Override
    public void handleRequest(Method protectedMethod, ApprovalContext context) {
        storeRequest(
                protectedMethod.getDeclaringClass().getName(),
                protectedMethod.getName(),
                context);

        // TODO rewrite this to some relevant event logging
        try {
            log.info(JsonSerialization.writeValueAsString(context.getRepresentation()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ApprovalRequestModel storeRequest(String requester, String action, ApprovalContext context) {
        ApprovalRequestModel request = requestStore.createRequest(requester, context.getRealm());

        try {
            request.setAttributeIfNotNull(ApprovalContext.REPRESENTATION_ATTR, JsonSerialization.writeValueAsString(context.getRepresentation()));
            request.setAttributeIfNotNull(ApprovalContext.ACTION_ATTR, action);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return request;
    }

    @Override
    public void handleResponse() {

    }

    @Override
    public void close() {

    }
}
