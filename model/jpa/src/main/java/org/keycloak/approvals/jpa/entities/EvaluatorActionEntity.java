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

import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Entity
@IdClass(EvaluatorActionEntity.Key.class)
@Table(name = "APPROVAL_EVALUATOR_ACTION")
@NamedQueries({
        @NamedQuery(name = "getEvaluatorActionById", query = "select e from EvaluatorActionEntity e where e.handlerId = :handlerId and e.actionId = :actionId and e.realm.id = :realmId")
})
public class EvaluatorActionEntity {
    @Id
    @Column(name = "HANDLER_ID", length = 255)
    private String handlerId;

    @Id
    @Column(name = "ACTION_ID", length = 255)
    private String actionId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realm;

    @Column(name = "ENABLED")
    boolean enabled;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "APPROVAL_EVALUATOR_ACTION_ROLES", joinColumns = {@JoinColumn(name = "HANDLER_ID", referencedColumnName = "HANDLER_ID"), @JoinColumn(name = "ACTION_ID", referencedColumnName = "ACTION_ID"), @JoinColumn(name = "REALM_ID", referencedColumnName = "REALM_ID")}, inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private Set<RoleEntity> roles = new HashSet<>();

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
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

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

    public static class Key implements Serializable {
        private String handlerId;
        private String actionId;
        private RealmEntity realm;

        public Key() {
        }

        public Key(String handlerId, String actionId, RealmEntity realm) {
            this.handlerId = handlerId;
            this.actionId = actionId;
            this.realm = realm;
        }

        public String getHandlerId() {
            return handlerId;
        }

        public String getActionId() {
            return actionId;
        }

        public RealmEntity getRealm() {
            return realm;
        }
    }
}
