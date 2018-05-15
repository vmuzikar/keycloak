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
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ApprovalAction;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

/**
 * Handles configuration-changing actions that are watched by the Approvals System.
 * <p>
 * Each implementation of this interface is meant to handle some set of related actions (e.g. users related, clients
 * related etc.).
 *
 * @see ApprovalManager
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalHandler extends Provider {
    /**
     * If an approval of some action is required, {@link ApprovalManager} should call this method.
     * This method prepares the system for the approval process (e.g. disables a user is an approval is required after
     * it's creation) and stores all the necessary data in the {@link ApprovalRequestRepresentation}. The stored data
     * contain only the information needed to apply the changes that were intercepted previously.
     *
     * @param context the context for an ongoing configuration-changing action
     * @return the Approval Request representation that contains information about the requested configuration change
     */
    ApprovalRequestRepresentation handleRequestCreation(ApprovalContext context);

    /**
     * After approving some Approval Request, this method is responsible for applying the changes that were intercepted
     * and required the approval. It's called by {@link ApprovalManager}.
     *
     * @param request the Approval Request which will be approved
     */
    void handleRequestApproval(ApprovalRequestModel request);

    /**
     * After rejection some Approval Request, this method is responsible for cleaning up of any changes that were made
     * by {@link #handleRequestCreation(ApprovalContext)}. E.g. if some newly created user was disabled prior to it's
     * approval, this method should remove it.
     *
     * @param request the Approval Request which will be rejected
     */
    void handleRequestRejection(ApprovalRequestModel request);

    /**
     * Retries a list of actions that are supported by the {@link ApprovalHandler} implementation.
     *
     * @return the supported actions list
     */
    ApprovalAction[] getSupportedActions();

    /**
     * Retrieves some action by its ID
     *
     * @param id the action identification
     * @return the action
     */
    ApprovalAction getActionById(String id);
}
