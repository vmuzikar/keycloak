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

package org.keycloak.approvals;

import org.keycloak.Config;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;

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
                RealmModel.RealmPreRemoveEvent realmRemovedEvent = (RealmModel.RealmPreRemoveEvent) event;
                ApprovalManager approvalManager = realmRemovedEvent.getKeycloakSession().getProvider(ApprovalManager.class);
                for (ApprovalRequestModel request : approvalManager.getStore().getRequestsForRealm(realmRemovedEvent.getRealm())) {
                    approvalManager.rejectRequest(request);
                }
                approvalManager.getStore().removeListenerConfigsForRealm(realmRemovedEvent.getRealm());
                approvalManager.getStore().removeRoleEvaluatorConfigsForRealm(realmRemovedEvent.getRealm());
            }
            if (event instanceof RoleContainerModel.RolePreRemoveEvent) {
                RoleContainerModel.RolePreRemoveEvent roleRemovedEvent = (RoleContainerModel.RolePreRemoveEvent) event;
                ApprovalManager approvalManager = roleRemovedEvent.getKeycloakSession().getProvider(ApprovalManager.class);
                approvalManager.getStore().removeRoleFromRoleEvaluatorConfigs(roleRemovedEvent.getRole());
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
