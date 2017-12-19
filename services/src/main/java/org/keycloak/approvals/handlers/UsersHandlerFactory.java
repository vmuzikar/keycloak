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

import org.keycloak.Config;
import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalHandler;
import org.keycloak.approvals.ApprovalHandlerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersHandlerFactory implements ApprovalHandlerFactory {
    @Override
    public ApprovalContext.Action[] getActions() {
        return UsersHandler.Actions.values();
    }

    @Override
    public ApprovalHandler create(KeycloakSession session) {
        return new UsersHandler(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return UsersHandler.HANDLER_ID;
    }
}
