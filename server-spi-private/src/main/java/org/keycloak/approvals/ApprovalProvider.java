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

package org.keycloak.approvals;

import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalRequestStore;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ApprovalProvider extends Provider {
    ApprovalRequestStore getRequestStore();
    ApprovalHandler getHandlerByProtectedClass(Class protectedClass);
    ApprovalHandler getHandlerByRequest(String requestId, RealmModel realmModel);
    ApprovalHandler getHandlerByRequest(ApprovalRequestModel requestModel);
    boolean approveRequest(String requestId, RealmModel realm);
}
