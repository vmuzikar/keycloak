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

import org.keycloak.approvals.ApprovalAction;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalStore extends Provider {
    ApprovalRequestModel createRequest(RealmModel realm, String handlerId);
    boolean removeRequest(String id, RealmModel realm);
    boolean removeRequest(ApprovalRequestModel requestModel);
    ApprovalRequestModel getRequestById(String id, RealmModel realm);
    List<ApprovalRequestModel> getRequestsForRealm(RealmModel realm);

    ApprovalListenerConfigModel createOrGetListenerConfig(String providerId, RealmModel realm);
    boolean removeListenerConfig(String providerId, RealmModel realm);

    RoleEvaluatorConfigModel createOrGetRoleEvaluatorConfig(ApprovalAction action, RealmModel realm);
    boolean removeRoleEvaluatorConfig(ApprovalAction action, RealmModel realm);
}
