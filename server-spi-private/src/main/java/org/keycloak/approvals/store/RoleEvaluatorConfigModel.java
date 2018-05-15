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
import org.keycloak.models.RoleModel;

import java.util.List;
import java.util.Set;

/**
 * Configuration for {@link org.keycloak.approvals.evaluators.RoleEvaluator} and a specific {@link ApprovalAction}.
 * It's basically a list of roles for a given {@link ApprovalAction}. Those roles then requires an approval for given
 * {@link ApprovalAction}.
 *
 * @see ApprovalStore
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface RoleEvaluatorConfigModel {
    /**
     * Retrieves the action.
     *
     * @return the action
     */
    ApprovalAction getAction();

    /**
     * Retrieves information if the configuration for current {@link ApprovalAction} is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Sets if the configuration for current {@link ApprovalAction} is enabled.
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Retrieves the list of roles that requires approval for current {@link ApprovalAction}.
     *
     * @return the list of roles.
     */
    List<RoleModel> getRoles();

    /**
     * Replaces the roles with new ones that are specified by their IDs.
     *
     * @param roles the role IDs
     */
    void setRolesByIds(Set<String> roles);

    /**
     * Adds a single role.
     *
     * @param roleId the role ID
     */
    void addRole(String roleId);
}
