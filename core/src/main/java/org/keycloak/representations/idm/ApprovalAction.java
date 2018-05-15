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

package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A representation of some configuration-changing action that's watched by the Approvals System.
 * It's intended to be implemented by an {@code enum}.
 *
 * @see org.keycloak.approvals.ApprovalManager
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface ApprovalAction {
    /**
     * The identification of a specific {@link org.keycloak.approvals.ApprovalHandler} implementation that is able of
     * handling this action.
     *
     * @return the provider id
     */
    String getHandlerId();

    /**
     * The identification of a specific {@link org.keycloak.approvals.ApprovalEvaluator} which should be used to evaluate
     * this action. This is optional. If no evaluator is specified, the default one specified by
     * {@link org.keycloak.approvals.ApprovalManager} is used.
     *
     * @return the provider id
     */
    String getEvaluatorId();

    /**
     * The identification of this action.
     *
     * @return the identification
     */
    String getActionId();

    /**
     * Retrieves the human readable description of this Approval Request. It's used in the UI.
     *
     * @return the description
     */
    String getDescription();
}
