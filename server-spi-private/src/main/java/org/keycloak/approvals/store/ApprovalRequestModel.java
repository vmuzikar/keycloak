/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalRequestModel {
    String getId();
    RealmModel getRealm();
    String getHandlerId();

    String getDescription();
    void setDescription(String description);

    String getActionId();
    void setActionId(String actionId);

    String getAttribute(String name);
    void setAttribute(String name, String value);
    void setAttributeIfNotNull(String name, String value);
    void removeAttribute(String name);
    void setAttributes(Map<String, String> attributes);
}
