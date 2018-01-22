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

import org.keycloak.Config;
import org.keycloak.approvals.ApprovalEvaluator;
import org.keycloak.approvals.ApprovalEvaluatorFactory;
import org.keycloak.approvals.ApprovalManager;
import org.keycloak.approvals.handlers.UsersHandler;
import org.keycloak.approvals.store.RoleEvaluatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RoleEvaluatorFactory implements ApprovalEvaluatorFactory {
    @Override
    public ApprovalEvaluator create(KeycloakSession session) {
        return new RoleEvaluator(session, session.getProvider(ApprovalManager.class).getStore());
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO remove testing data!!!!!!!
        factory.register(event -> {
            if (event instanceof RealmModel.RealmPostCreateEvent) {
                RealmModel.RealmPostCreateEvent realmEvent = (RealmModel.RealmPostCreateEvent) event;
                RealmModel realm = realmEvent.getCreatedRealm();
                RoleModel role = realm.addRole("approvals-role");
                RoleEvaluatorConfigModel config = realmEvent.getKeycloakSession().getProvider(ApprovalManager.class).getStore().createOrGetRoleEvaluatorConfig(UsersHandler.Actions.CREATE_USER, realm);
                config.addRole(role.getId());
                config.setEnabled(true);
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return RoleEvaluator.PROVIDER_ID;
    }
}
