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

import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalStore;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

import java.util.List;

/**
 * The central point for accessing and manipulating the Approvals System.
 * <p>
 * The system as such is designed to evaluate if given configuration-changing action (e.g. a user creation)
 * needs an approval, is able to intercept such action and then apply the configuration changes after an approval.
 * <p>
 * Each action needs to have specified which implementation of {@link ApprovalHandler} can handle it in the means of
 * Approval Request creation, and approving or rejecting of the request.
 *
 * @see org.keycloak.representations.idm.ApprovalAction
 * @see ApprovalHandler
 * @see ApprovalEvaluator
 * @see ApprovalContext
 * @see ApprovalListener
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalManager extends Provider {
    /**
     * Retrieves the persistence store for Approvals System.
     *
     * @return The persistence store.
     */
    ApprovalStore getStore();

    /**
     * Retrieves an {@link ApprovalHandler} instance for given Approval Request based on the request ID.
     * Each Approval Request has assigned it's own Approval Handler implementation that's able handle the request (approve, reject etc.).
     *
     * @see ApprovalRequestModel
     *
     * @param requestId The Approval Request identification.
     * @param realmModel the realm where the Approval Request belongs to.
     * @return The Approval Handler or {@code null} if the Approval Request or the Approval Handler implementation
     *         specified for that request don't exist.
     */
    ApprovalHandler getHandlerByRequest(String requestId, RealmModel realmModel);

    /**
     * Retrieves an {@link ApprovalHandler} instance for given Approval Request.
     *
     * @param requestModel The Approval Request model.
     * @return The Approval Handler or {@code null} if the Approval Handler implementation specified for that request
     *         doesn't exist.
     */
    ApprovalHandler getHandlerByRequest(ApprovalRequestModel requestModel);

    /**
     * Intercepts a configuration changing action if an approval is required, and creates an Approval Request.
     * <p>
     * Works in two steps.
     * <ul>
     *     <li>Evaluates if given action needs an approval.
     *     <li>If an approval is required, intercepts the action and creates an Approval Request.
     * </ul>
     *
     * @see ApprovalRequestModel
     * @see ApprovalHandler
     *
     * @param context the context for given action
     * @throws InterceptedException Should intercept the ongoing configuration-changing action.
     *                              Thrown only if an approval is required.
     */
    void interceptAction(ApprovalContext context) throws InterceptedException;

    /**
     * Creates an Approval Request based on it's representation.
     *
     * @param requestRep the Approval Request representation
     * @param realm the realm where the Approval Request should be placed to
     * @return Approval Request model
     */
    ApprovalRequestModel createRequest(ApprovalRequestRepresentation requestRep, RealmModel realm);

    /**
     * Approves given Approval Request based on the request ID and applies the configuration changes that were
     * interrupted previously.
     *
     * @see ApprovalHandler
     *
     * @param requestId the Approval Request identification
     * @param realm the realm where the Approval Request belongs to
     * @return the representation of the approved request or {@code null} if no request with such ID exists
     */
    ApprovalRequestRepresentation approveRequest(String requestId, RealmModel realm);

    /**
     * Approves given Approval Request and applies the configuration changes that were
     * interrupted previously.
     *
     * @see ApprovalHandler
     *
     * @param request the existing Approval Request
     * @return {@code true} if the request was successfully approved, {@code false} otherwise
     */
    boolean approveRequest(ApprovalRequestModel request);

    /**
     * Rejects given Approval Request based on the request ID and cleans the system after the rejection if necessary.
     *
     * @see ApprovalHandler
     *
     * @param requestId the Approval Request identification
     * @param realm the realm where the Approval Request belongs to
     * @return the representation of the rejected request or {@code null} if no request with such ID exists
     */
    ApprovalRequestRepresentation rejectRequest(String requestId, RealmModel realm);

    /**
     * Rejects given Approval Request based on the request ID and cleans the system after the rejection if necessary.
     *
     * @param request the existing Approval Request
     * @return {@code true} if the request was successfully rejected, {@code false} otherwise
     */
    boolean rejectRequest(ApprovalRequestModel request);

    /**
     * Retrieves a list of {@link ApprovalListener} implementations that are enabled for given realm
     *
     * @param realm
     * @return
     */
    List<ApprovalListener> getEnabledListeners(RealmModel realm);
}
