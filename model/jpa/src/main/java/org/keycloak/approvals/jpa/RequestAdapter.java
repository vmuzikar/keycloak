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

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RequestAdapter implements ApprovalRequestModel, JpaModel<RequestEntity> {
    private RequestEntity entity;
    private RealmModel realm;

    public RequestAdapter(RequestEntity entity, RealmModel realm) {
        this.entity = entity;
        this.realm = realm;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public RealmModel getRealm() {
        return null;
    }

    @Override
    public String getRequester() {
        return null;
    }

    @Override
    public String getAttribute(String name) {
        return null;
    }

    @Override
    public void setAttribute(String name, String value) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public RequestEntity getEntity() {
        return null;
    }
}
