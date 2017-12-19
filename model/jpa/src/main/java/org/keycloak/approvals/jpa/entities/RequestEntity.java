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

package org.keycloak.approvals.jpa.entities;

import org.keycloak.models.jpa.entities.RealmEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Entity
@Table(name = "APPROVAL_REQUESTS")
@NamedQueries({
        @NamedQuery(name = "getRequestById", query = "select r from RequestEntity r where r.id = :id and realm.id = :realmId"),
        @NamedQuery(name = "getAllRequestsForRealm", query = "select r.id from RequestEntity r where r.realm.id = :realmId")
})
public class RequestEntity {
    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realm;

    @Column(name = "HANDLER_ID")
    private String handlerId;

    @Column(name = "ACTION_ID")
    private String actionId;

    @ElementCollection
    @MapKeyColumn(name = "NAME")
    @Column(name = "VALUE", length = 4096)
    @CollectionTable(name = "APPROVAL_REQUEST_ATTRIBUTES", joinColumns = {@JoinColumn(name = "REQUEST_ID")})
    private Map<String, String> attributes = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String requester) {
        this.handlerId = requester;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
