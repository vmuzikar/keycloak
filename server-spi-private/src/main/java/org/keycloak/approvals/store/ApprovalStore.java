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
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * Persistence functionality for the Approval System.
 *
 * @see org.keycloak.approvals.ApprovalManager
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalStore extends Provider {
    /**
     * Stores an empty Approval Request without any specific requester.
     *
     * @param realm the realm where the action is taking place.
     * @param action the action
     * @return the newly created Approval Request
     */
    ApprovalRequestModel createRequest(RealmModel realm, ApprovalAction action);

    /**
     * Stores an empty Approval Request which is created by a specific requester.
     *
     * @param realm the realm where the action is taking place
     * @param action the action
     * @param user the requester
     * @param userRealm the requester's realm
     * @return the newly created Approval Request
     */
    ApprovalRequestModel createRequest(RealmModel realm, ApprovalAction action, UserModel user, RealmModel userRealm);

    /**
     * Removes stored Approval Request by ID.
     *
     * @param id the request identification
     * @param realm the realm where the Approval Request belongs to
     * @return {@code true} if the request was removed successfully, {@code false} otherwise
     */
    boolean removeRequest(String id, RealmModel realm);

    /**
     * Removes stored Approval Request.
     *
     * @param requestModel the request to remove
     * @return {@code true} if the request was removed successfully, {@code false} otherwise
     */
    boolean removeRequest(ApprovalRequestModel requestModel);

    /**
     * Gets stored Approval Request by ID.
     *
     * @param id the request identification
     * @param realm the realm where the Approval Request belongs to
     * @return the Approval Request or {@code null} if no such request exists
     */
    ApprovalRequestModel getRequestById(String id, RealmModel realm);

    /**
     * Gets all stored Approval Requests for a given realm.
     *
     * @param realm the realm where the Approval Requests belong to
     * @return the list of requests
     */
    List<ApprovalRequestModel> getRequestsForRealm(RealmModel realm);

    /**
     * Gets all stored Approval Requests for a given requester.
     *
     * @see #createRequest(RealmModel, ApprovalAction, UserModel, RealmModel)
     *
     * @param user the requester
     * @param realm the realm where the Approval Requests belong to
     * @return the list of requests
     */
    List<ApprovalRequestModel> getRequestsByUser(UserModel user, RealmModel realm);

    /**
     * Retrieves configuration for an {@link org.keycloak.approvals.ApprovalListener}. If no configuration exists,
     * the default one is created.
     *
     * @param providerId identification of an {@link org.keycloak.approvals.ApprovalListener} implementation
     * @param realm the realm for which is the configuration stored
     * @return the configuration
     */
    ApprovalListenerConfigModel createOrGetListenerConfig(String providerId, RealmModel realm);

    /**
     * Removes configurations of all {@link org.keycloak.approvals.ApprovalListener}s for given realm
     *
     * @param realm the realm from which should be the configurations removed
     * @return {@code true} if the configurations were removed successfully, {@code false} otherwise
     */
    boolean removeListenerConfigsForRealm(RealmModel realm);

    /**
     * Retrieves the specific configuration for {@link org.keycloak.approvals.evaluators.RoleEvaluator}.
     * If no configuration exists, the default one is created. Basically gets list of role for a given {@link ApprovalAction}
     *
     * @param action the action
     * @param realm the realm for which is the configuration stored
     * @return the configuration
     */
    RoleEvaluatorConfigModel createOrGetRoleEvaluatorConfig(ApprovalAction action, RealmModel realm);

    /**
     * Removes configurations of {@link org.keycloak.approvals.evaluators.RoleEvaluator} for a realm.
     *
     * @param realm the realm
     * @return {@code true} if the configurations were removed successfully, {@code false} otherwise
     */
    boolean removeRoleEvaluatorConfigsForRealm(RealmModel realm);

    /**
     * Removes a role from all configurations of {@link org.keycloak.approvals.evaluators.RoleEvaluator}.
     *
     * @param role the role
     * @return {@code true} if the configurations were removed successfully, {@code false} otherwise
     */
    boolean removeRoleFromRoleEvaluatorConfigs(RoleModel role);
}