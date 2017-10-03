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

import org.keycloak.approvals.jpa.entities.RequestEntity;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalRequestStore;
import org.keycloak.approvals.store.ApprovalStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class JpaRequestStore implements ApprovalRequestStore {
    protected KeycloakSession session;
    protected EntityManager em;
    protected ApprovalStoreProvider storeProvider;

    public JpaRequestStore(KeycloakSession session, EntityManager em, ApprovalStoreProvider storeProvider) {
        this.session = session;
        this.em = em;
        this.storeProvider = storeProvider;
    }

    @Override
    public ApprovalRequestModel createRequest(String requester, RealmModel realm) {
        RequestEntity entity = new RequestEntity();
        entity.setRequester(requester);
        entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));

        em.persist(entity);
        em.flush();

        return new RequestAdapter(entity, realm);
    }

    @Override
    public void removeRequest(String id, RealmModel realm) {

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

        return new RequestAdapter(entity, realm);
    }

    @Override
    public List<ApprovalRequestModel> getRequestsForRealm(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getRequestById", String.class);
        query.setParameter("realmId", realm.getId());

        List<String> ids = query.getResultList();

        return ids.stream()
                .map(id -> storeProvider.getRequestStore().getRequestById(id, realm))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {

    }
}
