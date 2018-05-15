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

package org.keycloak.approvals.store;

import org.keycloak.representations.idm.ApprovalAction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Date;
import java.util.Map;

/**
 * A single Approval Request. Represents a configuration-change request that needs an approval.
 * 
 * @see org.keycloak.approvals.ApprovalManager
 * 
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalRequestModel {
    /**
     * Retrieves the ID of this Approval Request.
     * 
     * @return the ID
     */
    String getId();

    /**
     * Retrieves the realm where this Approval Request belongs to.
     * 
     * @return the realm
     */
    RealmModel getRealm();

    /**
     * Retrieves the requested action.
     * 
     * @return the action
     */
    ApprovalAction getAction();

    /**
     * Retrieves the human readable description of this Approval Request. It's used in the UI.
     * 
     * @return the description
     */
    String getDescription();

    /**
     * Sets the description.
     * 
     * @see #getDescription()
     * 
     * @param description
     */
    void setDescription(String description);

    /**
     * Retrieves the time of creation for this Approval Request.
     * 
     * @return the creation time
     */
    Date getTime();

    /**
     * Retrieves the user that requests the configuration changes.
     *
     * @return the requester or {@code null} if no requester is specified
     */
    UserModel getUser();

    /**
     * Retrieves the requester's realm.
     *
     * @see #getUser()
     *
     * @return the realm or {@code null} if no requester is specified
     */
    RealmModel getUserRealm();

    /**
     * Retrieves an attribute of this Approval Request by key.
     *
     * @param name the key
     * @return the attribute or {@code null} if no attribute under this key is stored
     */
    String getAttribute(String name);

    /**
     * Stores an attribute under a key. If one already exists, replaces it.
     *
     * @param name the key
     * @param value the value of the attribute
     */
    void setAttribute(String name, String value);

    /**
     * Stores an attribute under a key. If one already exists, replaces it. If {@code value} is {@code null},
     * don't store it.
     *
     * @param name the key
     * @param value the value of the attribute
     */
    void setAttributeIfNotNull(String name, String value);

    /**
     * Remove a store attribute.
     *
     * @param name the key
     */
    void removeAttribute(String name);

    /**
     * Retrieves all attributes of this Approval Request.
     *
     * @return the map of attributes
     */
    Map<String, String> getAttributes();

    /**
     * Store multiple attributes and replace the old ones.
     *
     * @param attributes the map of attributes
     */
    void setAttributes(Map<String, String> attributes);
}
