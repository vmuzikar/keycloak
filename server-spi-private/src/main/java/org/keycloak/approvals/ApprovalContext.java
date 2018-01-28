/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.approvals;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApprovalAction;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalContext {
    public static final String REPRESENTATION_ATTR = "representation";
    public static final String MODEL_ATTR = "model";

    private RealmModel realm;
    private ApprovalAction action;
    private Map<String, Object> attributes = new HashMap<>();

    public ApprovalContext(RealmModel realm, ApprovalAction action) {
        setRealm(realm);
        setAction(action);
    }

    public static ApprovalContext withRepresentation(RealmModel realm, ApprovalAction action, Object representation) {
        return (new ApprovalContext(realm, action)).setRepresentation(representation);
    }

    public static ApprovalContext withModel(RealmModel realm, ApprovalAction action, Object model) {
        return (new ApprovalContext(realm, action)).setModel(model);
    }

    public ApprovalContext setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }


    public RealmModel getRealm() {
        return realm;
    }

    public ApprovalContext setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public ApprovalContext setAction(ApprovalAction action) {
        this.action = action;
        return this;
    }

    public Object getRepresentation() {
        return getAttribute(REPRESENTATION_ATTR);
    }

    public ApprovalContext setRepresentation(Object representation) {
        setAttribute(REPRESENTATION_ATTR, representation);
        return this;
    }

    public Object getModel() {
        return getAttribute(MODEL_ATTR);
    }

    public ApprovalContext setModel(Object model) {
        setAttribute(MODEL_ATTR, model);
        return this;
    }
}
