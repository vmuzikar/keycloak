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

import org.keycloak.approvals.jpa.entities.ListenerConfigEntity;
import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.JpaModel;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ListenerConfigAdapter implements ApprovalListenerConfigModel, JpaModel<ListenerConfigEntity> {
    private ListenerConfigEntity entity;
    private EntityManager em;
    private RealmModel realm;

    public ListenerConfigAdapter(ListenerConfigEntity entity, EntityManager em, RealmModel realm) {
        this.entity = entity;
        this.em = em;
        this.realm = realm;
    }

    @Override
    public String getProviderId() {
        return entity.getProviderId();
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
        em.flush();
    }

    @Override
    public Map<String, String> getConfigs() {
        return new HashMap<>(entity.getConfigs());
    }

    @Override
    public void setConfigs(Map<String, String> configs) {
        entity.setConfigs(configs);
        em.flush();
    }

    @Override
    public void mergeConfigs(Map<String, String> configs) {
        entity.getConfigs().putAll(configs);
        em.flush();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ListenerConfigEntity getEntity() {
        return entity;
    }
}
