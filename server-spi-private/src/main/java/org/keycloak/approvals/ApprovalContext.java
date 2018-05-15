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
 * The context for currently processed action (e.g. a user creation). Carries all the necessary information (like
 * representations and models) for an action description.
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
     * @return The retrieved attribute or {@code null} if no attribute is stored in the context for given key.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }


    /**
     * Retrieves the target realm where the action is taking place.
     *
     * @return The realm.
     */
    public RealmModel getRealm() {
        return realm;
    }

    /**
     * Sets the target realm where the action is taking place.
     *
     * @param realm
     * @return The context.
     */
    public ApprovalContext setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    /**
     * Retrieves the action.
     * @see ApprovalAction
     *
     * @return The action.
     */
    public ApprovalAction getAction() {
        return action;
    }

    /**
     * Sets the action.
     *
     * @param action
     * @return This context.
     */
    public ApprovalContext setAction(ApprovalAction action) {
        this.action = action;
        return this;
    }

    /**
     * Retrieves the representation attribute stored in this context.
     * This is a shortcut for {@code getAttribute(REPRESENTATION_ATTR)}.
     * A representation can be any DTO, e.g. {@link org.keycloak.representations.idm.UserRepresentation}.
     *
     * @see #getAttribute(String)
     *
     * @return A representation or {@code null} if no representation was stored in the context.
     */
    public Object getRepresentation() {
        return getAttribute(REPRESENTATION_ATTR);
    }

    /**
     * Stores a representation attribute.
     * This is a shortcut for {@code setAttribute(REPRESENTATION_ATTR, representation)}.
     * A representation can be any DTO, e.g. {@link org.keycloak.representations.idm.UserRepresentation}.
     *
     * @see #setAttribute(String, Object)
     *
     * @param representation
     * @return This context.
     */
    public ApprovalContext setRepresentation(Object representation) {
        setAttribute(REPRESENTATION_ATTR, representation);
        return this;
    }

    /**
     * Retrieves the model attribute stored in this context.
     * This is a shortcut for {@code getAttribute(MODEL_ATTR)}.
     * A model can be any DAO, e.g. {@link org.keycloak.models.UserModel}.
     *
     * @see #getAttribute(String)
     *
     * @return A model or {@code null} if no model was stored in the context.
     */
    public Object getModel() {
        return getAttribute(MODEL_ATTR);
    }

    /**
     * Stores a model attribute.
     * This is a shortcut for {@code setAttribute(MODEL_ATTR, model)}
     * A model can be any DAO, e.g. {@link org.keycloak.models.UserModel}.
     *
     * @see #setAttribute(String, Object)
     *
     * @param model
     * @return This context.
     */
    public ApprovalContext setModel(Object model) {
        setAttribute(MODEL_ATTR, model);
        return this;
    }
}
