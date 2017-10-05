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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalContext {
    public static final String REALM_ATTR = "realm";
    public static final String REPRESENTATION_ATTR = "representation";
    public static final String MODEL_ATTR = "model";
    public static final String ACTION_ATTR = "action";

    private Map<String, Object> attributes = new HashMap<>();

    protected ApprovalContext() {
    }


    // Factories

    public static ApprovalContext empty(RealmModel realm) {
        return new ApprovalContext().setAttribute(REALM_ATTR, realm);
    }

    public static ApprovalContext fromRep(Object representation, RealmModel realm) {
        return empty(realm).setAttribute(REPRESENTATION_ATTR, representation);
    }

    public static ApprovalContext fromModel(Object model, RealmModel realm) {
        return empty(realm).setAttribute(MODEL_ATTR, model);
    }


    // Generic getter and setter

    public ApprovalContext setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }


    // Some common getters/setters

    public String getAction() {
        return (String)getAttribute(ACTION_ATTR);
    }

    public ApprovalContext setAction(String value) {
        setAttribute(ACTION_ATTR, value);
        return this;
    }

    public RealmModel getRealm() {
        return (RealmModel)getAttribute(REALM_ATTR);
    }

    public Object getRepresentation() {
        return getAttribute(REPRESENTATION_ATTR);
    }

    public Object getModel() {
        return getAttribute(MODEL_ATTR);
    }
}
