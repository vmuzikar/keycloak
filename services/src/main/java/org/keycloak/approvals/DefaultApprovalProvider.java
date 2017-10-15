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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalProvider implements ApprovalProvider {
    protected KeycloakSession session;

    public DefaultApprovalProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ApprovalRequestStore getRequestStore() {
        return session.getProvider(ApprovalRequestStore.class); // TODO caching layer...
    }

    @Override
    public ApprovalHandler getHandlerByProtectedClass(Class protectedClass) {
        return ((ApprovalHandlerFactory)session
                .getKeycloakSessionFactory()
                .getProviderFactory(ApprovalHandler.class))
                .create(session, protectedClass);
    }

    @Override
    public ApprovalHandler getHandlerByRequest(String requestId, RealmModel realmModel) {
        ApprovalRequestModel request = getRequestStore().getRequestById(requestId, realmModel);

        if (request == null) {
            return null;
        }

        return getHandlerByRequest(request);
    }

    @Override
    public ApprovalHandler getHandlerByRequest(ApprovalRequestModel requestModel) {
        Class protectedClass;
        try {
            protectedClass = Class.forName(requestModel.getRequester());
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return getHandlerByProtectedClass(protectedClass);
    }

    @Override
    public void close() {

    }
}
