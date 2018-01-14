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

package org.keycloak.approvals.jpa.entities;

import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Entity
@IdClass(ListenerConfigEntity.Key.class)
@Table(name = "APPROVAL_LISTENER_CONFIGS")
@NamedQueries({
        @NamedQuery(name = "getListenerById", query = "select l from ListenerConfigEntity l where l.providerId = :providerId and realm.id = :realmId")
})
public class ListenerConfigEntity {
    @Id
    @Column(name = "PROVIDER_ID", length = 255)
    private String providerId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realm;

    @Column(name = "ENABLED")
    boolean enabled;

    @ElementCollection
    @MapKeyColumn(name = "NAME")
    @Column(name = "VALUE", length = 4096)
    @CollectionTable(name = "APPROVAL_LISTENER_CONFIGS_VALUES", joinColumns = {@JoinColumn(name = "PROVIDER_ID", referencedColumnName = "PROVIDER_ID"), @JoinColumn(name = "REALM_ID", referencedColumnName = "REALM_ID")})
    private Map<String, String> configs = new HashMap<>();

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    public static class Key implements Serializable {
        private String providerId;
        private RealmEntity realm;

        public Key() {
        }

        public Key(String providerId, RealmEntity realm) {
            this.providerId = providerId;
            this.realm = realm;
        }

        public String getProviderId() {
            return providerId;
        }

        public RealmEntity getRealm() {
            return realm;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListenerConfigEntity entity = (ListenerConfigEntity) o;
        return Objects.equals(providerId, entity.providerId) &&
                Objects.equals(realm, entity.realm);
    }

    @Override
    public int hashCode() {

        return Objects.hash(providerId, realm);
    }
}
