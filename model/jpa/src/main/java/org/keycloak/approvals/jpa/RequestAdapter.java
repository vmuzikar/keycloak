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
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.JpaModel;

import javax.persistence.EntityManager;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RequestAdapter implements ApprovalRequestModel, JpaModel<RequestEntity> {
    private RequestEntity entity;
    private EntityManager em;
    private RealmModel realm;

    public RequestAdapter(RequestEntity entity, EntityManager em, RealmModel realm) {
        this.entity = entity;
        this.em = em;
        this.realm = realm;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getHandlerId() {
        return entity.getHandlerId();
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
        em.flush();
    }

    @Override
    public String getActionId() {
        return entity.getActionId();
    }

    @Override
    public void setActionId(String actionId) {
        entity.setActionId(actionId);
        em.flush();
    }

    @Override
    public String getAttribute(String name) {
        return entity.getAttributes().get(name);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.getAttributes().put(name, value);
        em.flush();
    }

    @Override
    public void setAttributeIfNotNull(String name, String value) {
        if (value != null) {
            setAttribute(name, value);
            em.flush();
        }
    }

    @Override
    public void removeAttribute(String name) {
        entity.getAttributes().remove(name);
        em.flush();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        entity.setAttributes(attributes);
        em.flush();
    }

    @Override
    public RequestEntity getEntity() {
        return entity;
    }
}
