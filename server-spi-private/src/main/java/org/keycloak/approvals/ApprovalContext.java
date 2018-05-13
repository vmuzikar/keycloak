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

package org.keycloak.approvals;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApprovalAction;

import java.util.HashMap;
import java.util.Map;

/**
 * The context for currently processed action (e.g. creating a user). Carries all the necessary information (like
 * representations and models) for evaluating an action, creating an approval request, approving an action etc.
 *
 * @see ApprovalEvaluator
 * @see ApprovalHandler
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalContext {
    /**
     * The key for storing a representation attribute.
     */
    public static final String REPRESENTATION_ATTR = "representation";

    /**
     * The key for storing a model attribute.
     */
    public static final String MODEL_ATTR = "model";

    private RealmModel realm;
    private ApprovalAction action;
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Context with no preset attributes.
     *
     * @param realm the realm where the action is taking place
     * @param action the processed action
     */
    public ApprovalContext(RealmModel realm, ApprovalAction action) {
        setRealm(realm);
        setAction(action);
    }

    /**
     * Creates a context with preset representation attribute.
     *
     * @param realm the realm where the action is taking place
     * @param action the processed action
     * @param representation the representation of some entity (e.g. {@link org.keycloak.representations.idm.UserRepresentation})
     * @return The newly created context.
     */
    public static ApprovalContext withRepresentation(RealmModel realm, ApprovalAction action, Object representation) {
        return (new ApprovalContext(realm, action)).setRepresentation(representation);
    }

    /**
     * Creates a context with preset model attribute.
     *
     * @param realm the realm where the action is taking place
     * @param action the processed action
     * @param model the model for some entity (e.g. {@link org.keycloak.models.UserModel})
     * @return The newly created context.
     */
    public static ApprovalContext withModel(RealmModel realm, ApprovalAction action, Object model) {
        return (new ApprovalContext(realm, action)).setModel(model);
    }

    /**
     * Stores an attribute in this context.
     *
     * @param name the key for storing the attribute
     * @param value the attribute
     * @return This context.
     */
    public ApprovalContext setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    /**
     * Retrieves an attribute stored in this context.
     *
     * @param name the key for storing the attribute
     * @return The retrieved attribute or null if no attribute is stored in the context for given key.
     */
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
