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

package org.keycloak.approvals.jpa;

import org.keycloak.approvals.ApprovalAction;
import org.keycloak.approvals.ApprovalHandler;
import org.keycloak.approvals.jpa.entities.EvaluatorActionEntity;
import org.keycloak.approvals.store.RoleEvaluatorMappingModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.JpaModel;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RoleEvaluatorMappingAdapter implements RoleEvaluatorMappingModel, JpaModel<EvaluatorActionEntity> {
    private EvaluatorActionEntity entity;
    private EntityManager em;
    private RealmModel realm;
    private ApprovalAction action = null;
    private KeycloakSession session;

    public RoleEvaluatorMappingAdapter(EvaluatorActionEntity entity, EntityManager em, RealmModel realm, KeycloakSession session) {
        this.entity = entity;
        this.em = em;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public ApprovalAction getAction() {
        if (action == null) {
            action = session.getProvider(ApprovalHandler.class, entity.getHandlerId()).getActionById(entity.getActionId());
        }
        return action;
    }

    @Override
    public boolean getEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public List<RoleModel> getRoles() {
        return null;
    }

    @Override
    public void setRolesByIds(List<String> roles) {

    }

    @Override
    public void addRole(String roleId) {

    }

    @Override
    public EvaluatorActionEntity getEntity() {
        return null;
    }
}
