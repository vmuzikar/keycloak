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

import org.keycloak.models.RealmModel;

import java.util.Map;

/**
 * The general configuration for a single {@link org.keycloak.approvals.ApprovalListener} implementation.
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalListenerConfigModel {
    /**
     * The identification of the specific {@link org.keycloak.approvals.ApprovalListener} implementation.
     *
     * @return the identification
     */
    String getProviderId();

    /**
     * Determines if the Approval Listener is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Sets if the Approval Listener is enabled.
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Retrieves all configuration as a key-value pairs for the Approval Listener.
     *
     * @return the configuration
     */
    Map<String, String> getConfigs();

    /**
     * Replace all the configuration by new one.
     *
     * @param configs key-value paired configuration
     */
    void setConfigs(Map<String, String> configs);

    /**
     * Merge configuration with the current one. Replace conflicts.
     *
     * @param configs key-value paired configuration
     */
    void mergeConfigs(Map<String, String> configs);

    /**
     * Retrieves the realm for which is the configuration stored.
     *
     * @return the realm
     */
    RealmModel getRealm();
}
