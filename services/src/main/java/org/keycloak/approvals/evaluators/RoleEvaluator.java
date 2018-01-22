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

package org.keycloak.approvals.evaluators;

import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalEvaluator;
import org.keycloak.approvals.store.ApprovalStore;
import org.keycloak.approvals.store.RoleEvaluatorConfigModel;
import org.keycloak.authorization.common.UserModelIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RoleEvaluator implements ApprovalEvaluator {
    public static final String PROVIDER_ID = "role";

    protected KeycloakSession session;
    protected ApprovalStore store;

    public RoleEvaluator(KeycloakSession session, ApprovalStore store) {
        this.session = session;
        this.store = store;
    }

    @Override
    public boolean needsApproval(ApprovalContext context) {
        RoleEvaluatorConfigModel config = store.createOrGetRoleEvaluatorConfig(context.getAction(), context.getRealm());

        if (!config.isEnabled()) {
            return false;
        }

        Identity identity = getIdentity(context);
        if (identity == null) {
            return true;
        }

        for (RoleModel role : config.getRoles()) {
            if (identity.hasRealmRole(role.getName())) {
                return true;
            }
        }

        return false;
    }

    protected Identity getIdentity(ApprovalContext context) {
        AppAuthManager authManager = new AppAuthManager();
        KeycloakContext kcContext = session.getContext();
        AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(session, context.getRealm(), kcContext.getUri(), kcContext.getConnection(), kcContext.getRequestHeaders());
        if (authResult == null) {
            return null;
        }
        return new UserModelIdentity(kcContext.getRealm(), authResult.getUser());
    }

    @Override
    public void close() {

    }
}
