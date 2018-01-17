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
import org.keycloak.approvals.jpa.entities.EvaluatorActionEntity;
import org.keycloak.approvals.store.EvaluatorStore;
import org.keycloak.approvals.store.RoleEvaluatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class JpaEvaluatorStore implements EvaluatorStore {
    protected KeycloakSession session;
    protected EntityManager em;

    public JpaEvaluatorStore(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    private EvaluatorActionEntity getEvaluatorActionEntity(ApprovalAction action, RealmModel realm) {
        TypedQuery<EvaluatorActionEntity> query = em.createNamedQuery("getEvaluatorActionById", EvaluatorActionEntity.class);
        query.setParameter("handlerId", action.getHandlerId());
        query.setParameter("actionId", action.getActionId());
        query.setParameter("realmId", realm.getId());

        EvaluatorActionEntity entity;
        try {
            entity = query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }

        return entity;
    }

    @Override
    public RoleEvaluatorConfigModel createOrGetRoleEvaluatorConfig(ApprovalAction action, RealmModel realm) {
        EvaluatorActionEntity entity = getEvaluatorActionEntity(action, realm);

        if (entity == null) {
            entity = new EvaluatorActionEntity();
            entity.setHandlerId(action.getHandlerId());
            entity.setActionId(action.getActionId());
            entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));
            entity.setEnabled(false);
            em.persist(entity);
            em.flush();
        }

        return new RoleEvaluatorConfigAdapter(entity, em, realm, session);
    }

    @Override
    public boolean removeRoleEvaluatorConfig(ApprovalAction action, RealmModel realm) {
        EvaluatorActionEntity entity = getEvaluatorActionEntity(action, realm);

        if (entity == null) {
            return false;
        }

        em.remove(entity);
        em.flush();

        return true;
    }

    @Override
    public void close() {

    }
}
