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

import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.provider.Provider;

/**
 * An SPI which listens to all Approvals System related operations.
 *
 * @see ApprovalManager
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalListener extends Provider {
    /**
     * Is called after an Approval Request creation, i.e. after intercepting of some configuration-changing action that
     * requires an approval.
     *
     * @param request the newly created Approval Request
     * @param context the context for an ongoing configuration-changing action
     */
    void afterRequestCreation(ApprovalRequestModel request, ApprovalContext context);

    /**
     * Is called after approving of some Approval Request, i.e. after the configuration changes are applied.
     *
     * @param request the Approval Request that was recently approved
     */
    void afterRequestApproval(ApprovalRequestModel request);

    /**
     * Is called after rejecting of some Approval Action.
     *
     * @param request the Approval Request that was recently rejected
     */
    void afterRequestRejection(ApprovalRequestModel request);

    /**
     * Retrieves the configuration for the {@link ApprovalListener} implementation.
     *
     * @return the configuration
     */
    ApprovalListenerConfigModel getConfig();

    /**
     * Stores the configuration in the {@link ApprovalListener} implementation.
     *
     * @param config
     */
    void setConfig(ApprovalListenerConfigModel config);
}
