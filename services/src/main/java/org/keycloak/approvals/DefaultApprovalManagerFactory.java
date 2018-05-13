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

package org.keycloak.approvals;

import org.keycloak.Config;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.UserModel;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalManagerFactory implements ApprovalManagerFactory {
    @Override
    public ApprovalManager create(KeycloakSession session) {
        return new DefaultApprovalManager(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof RealmModel.RealmPreRemoveEvent) {
                RealmModel.RealmPreRemoveEvent realmPreRemoveEvent = (RealmModel.RealmPreRemoveEvent) event;
                ApprovalManager approvalManager = realmPreRemoveEvent.getKeycloakSession().getProvider(ApprovalManager.class);
                for (ApprovalRequestModel request : approvalManager.getStore().getRequestsForRealm(realmPreRemoveEvent.getRealm())) {
                    approvalManager.rejectRequest(request);
                }
                approvalManager.getStore().removeListenerConfigsForRealm(realmPreRemoveEvent.getRealm());
                approvalManager.getStore().removeRoleEvaluatorConfigsForRealm(realmPreRemoveEvent.getRealm());
            }
            if (event instanceof RoleContainerModel.RolePreRemoveEvent) {
                RoleContainerModel.RolePreRemoveEvent rolePreRemoveEvent = (RoleContainerModel.RolePreRemoveEvent) event;
                ApprovalManager approvalManager = rolePreRemoveEvent.getKeycloakSession().getProvider(ApprovalManager.class);
                approvalManager.getStore().removeRoleFromRoleEvaluatorConfigs(rolePreRemoveEvent.getRole());
            }
            if (event instanceof UserModel.UserPreRemoveEvent) {
                UserModel.UserPreRemoveEvent userPreRemoveEvent = (UserModel.UserPreRemoveEvent) event;
                ApprovalManager approvalManager = userPreRemoveEvent.getKeycloakSession().getProvider(ApprovalManager.class);
                for (ApprovalRequestModel request : approvalManager.getStore().getRequestsByUser(userPreRemoveEvent.getUser(), userPreRemoveEvent.getRealm())) {
                    approvalManager.rejectRequest(request);
                }
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }
}
