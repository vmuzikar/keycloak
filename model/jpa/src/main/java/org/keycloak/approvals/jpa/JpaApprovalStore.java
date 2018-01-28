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

package org.keycloak.approvals.jpa;

import org.keycloak.approvals.ApprovalAction;
import org.keycloak.approvals.ApprovalListener;
import org.keycloak.approvals.ApprovalListenerFactory;
import org.keycloak.approvals.ApprovalManager;
import org.keycloak.approvals.jpa.entities.EvaluatorActionEntity;
import org.keycloak.approvals.jpa.entities.ListenerConfigEntity;
import org.keycloak.approvals.jpa.entities.RequestEntity;
import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalStore;
import org.keycloak.approvals.store.RoleEvaluatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class JpaApprovalStore implements ApprovalStore {
    protected KeycloakSession session;
    protected EntityManager em;
    protected ApprovalManager storeProvider;

    public JpaApprovalStore(KeycloakSession session, EntityManager em, ApprovalManager storeProvider) {
        this.session = session;
        this.em = em;
        this.storeProvider = storeProvider;
    }

    @Override
    public ApprovalRequestModel createRequest(RealmModel realm, String handlerId) {
        return createRequest(realm, handlerId, null, null);
    }

    @Override
    public ApprovalRequestModel createRequest(RealmModel realm, String handlerId, UserModel user, RealmModel userRealm) {
        RequestEntity entity = new RequestEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setHandlerId(handlerId);
        entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));
        entity.setTime(new Date());
        if (user != null && userRealm != null) {
            entity.setUser(em.getReference(UserEntity.class, user.getId()));
            entity.setUserRealm(em.getReference(RealmEntity.class, userRealm.getId()));
        }

        em.persist(entity);
        em.flush();

        return new RequestAdapter(entity, em, realm, user, userRealm);
    }

    @Override
    public boolean removeRequest(String id, RealmModel realm) {
        return removeRequest(getRequestById(id, realm));
    }

    @Override
    public boolean removeRequest(ApprovalRequestModel requestModel) {
        if (requestModel == null) {
            return false;
        }

        RequestEntity entity = ((RequestAdapter)requestModel).getEntity();
        em.remove(entity);
        em.flush();

        return true;
    }

    @Override
    public ApprovalRequestModel getRequestById(String id, RealmModel realm) {
        TypedQuery<RequestEntity> query = em.createNamedQuery("getRequestById", RequestEntity.class);
        query.setParameter("id", id);
        query.setParameter("realmId", realm.getId());

        RequestEntity entity;
        try {
            entity = query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }

        UserModel user = null;
        RealmModel userRealm = null;
        if (entity.getUser() != null && entity.getUserRealm() != null) {
            userRealm = session.realms().getRealm(entity.getUserRealm().getId());
            user = session.users().getUserById(entity.getUser().getId(), userRealm);
        }

        return new RequestAdapter(entity, em, realm, user, userRealm);
    }

    @Override
    public List<ApprovalRequestModel> getRequestsForRealm(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getAllRequestsForRealm", String.class);
        query.setParameter("realmId", realm.getId());

        List<String> ids = query.getResultList();

        return ids.stream()
                .map(id -> storeProvider.getStore().getRequestById(id, realm))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRequestModel> getRequestsByUser(UserModel user, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getAllRequestsByUser", String.class);
        query.setParameter("userId", user.getId());

        List<String> ids = query.getResultList();

        return ids.stream()
                .map(id -> storeProvider.getStore().getRequestById(id, realm))
                .collect(Collectors.toList());
    }

    private ListenerConfigEntity getListenerConfigEntity(String providerId, RealmModel realm) {
        TypedQuery<ListenerConfigEntity> query = em.createNamedQuery("getListenerById", ListenerConfigEntity.class);
        query.setParameter("providerId", providerId);
        query.setParameter("realmId", realm.getId());

        ListenerConfigEntity entity;
        try {
            entity = query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }

        return entity;
    }

    @Override
    public ApprovalListenerConfigModel createOrGetListenerConfig(String providerId, RealmModel realm) {
        ListenerConfigEntity entity = getListenerConfigEntity(providerId, realm);

        if (entity == null) {
            ApprovalListenerFactory listenerFactory = (ApprovalListenerFactory) session.getKeycloakSessionFactory().getProviderFactory(ApprovalListener.class, providerId);

            entity = new ListenerConfigEntity();
            entity.setProviderId(providerId);
            entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));
            entity.setEnabled(listenerFactory.enabledByDefault());
            entity.setConfigs(listenerFactory.getDefaultConfigs());
            em.persist(entity);
            em.flush();
        }

        return new ListenerConfigAdapter(entity, em, realm);
    }

    @Override
    public boolean removeListenerConfigsForRealm(RealmModel realm) {
        // Need to do it one-by-one due to CollectionTable
        TypedQuery<ListenerConfigEntity> query = em.createNamedQuery("getListenersByRealm", ListenerConfigEntity.class);
        query.setParameter("realmId", realm.getId());
        List<ListenerConfigEntity> entities = query.getResultList();
        entities.forEach(e -> em.remove(e));
        em.flush();

        return entities.size() > 0;
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
    public boolean removeRoleEvaluatorConfigsForRealm(RealmModel realm) {
        // Need to do it one-by-one due to CollectionTable
        TypedQuery<EvaluatorActionEntity> query = em.createNamedQuery("getEvaluatorActionsByRealm", EvaluatorActionEntity.class);
        query.setParameter("realmId", realm.getId());
        List<EvaluatorActionEntity> entities = query.getResultList();
        entities.forEach(e -> em.remove(e));
        em.flush();

        return entities.size() > 0;
    }

    @Override
    public boolean removeRoleFromRoleEvaluatorConfigs(RoleModel role) {
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        TypedQuery<EvaluatorActionEntity> query = em.createNamedQuery("getEvaluatorActionsWithRoles", EvaluatorActionEntity.class);
        query.setParameter("role", roleEntity);
        List<EvaluatorActionEntity> actionEntities = query.getResultList();

        boolean ret = false;
        for (EvaluatorActionEntity actionEntity : actionEntities) {
            for (RoleEntity roleFound : actionEntity.getRoles()) {
                if (roleFound.equals(roleEntity)) {
                    ret = true;
                    actionEntity.getRoles().remove(roleFound);
                }
            }
        }

        em.flush();
        return ret;
    }

    @Override
    public void close() {

    }
}
