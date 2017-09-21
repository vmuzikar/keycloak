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
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractApprovalHandler implements ApprovalHandler {
    private static final Logger log = Logger.getLogger(UsersResource.class);

    @Override
    public void handleRequest(Method protectedMethod, ApprovalContext context) {
        // TODO store the request
        try {
            log.info(JsonSerialization.writeValueAsString(context.getRepresentation()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleResponse() {

    }

    @Override
    public void close() {

    }
}
