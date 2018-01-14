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
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

import java.util.HashMap;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalManager implements ApprovalManager {
    protected KeycloakSession session;

    public DefaultApprovalManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ApprovalRequestStore getRequestStore() {
        return session.getProvider(ApprovalRequestStore.class); // TODO caching layer...
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
        return session.getProvider(ApprovalHandler.class, requestModel.getHandlerId());
    }

    @Override
    public void interceptAction(ApprovalContext context) throws InterceptedException {
        ApprovalHandler handler = session.getProvider(ApprovalHandler.class, context.getHandlerId());

        ApprovalEvaluator evaluator = handler.getEvaluator(context);
        if (evaluator == null) {
            evaluator = session.getProvider(ApprovalEvaluator.class, DefaultApprovalEvaluator.PROVIDER_ID);
        }

        if (evaluator.needsApproval(context)) {
            boolean tx = false;
            KeycloakTransactionManager tm = session.getTransactionManager(); // TODO Is that necessary?
            if (!tm.isActive()) {
                tx = true;
                tm.begin();
            }

            ApprovalRequestRepresentation requestRep = handler.handleRequestCreation(context);
            ApprovalRequestModel requestModel = createRequest(requestRep, context.getRealm());

            for (ApprovalListener listener : session.getAllProviders(ApprovalListener.class)) { // TODO Priorities, enabling?
                listener.afterRequestCreation(requestModel, context);
            }

            if (tx) {
                tm.commit();
            }

            throw new InterceptedException();
        }
    }

    @Override
    public ApprovalRequestModel createRequest(ApprovalRequestRepresentation requestRep, RealmModel realm) {
        ApprovalRequestModel requestModel = getRequestStore().createRequest(realm, requestRep.getHandlerId());

        requestModel.setActionId(requestRep.getActionId());
        requestModel.setDescription(requestRep.getDescription());
        requestModel.setAttributes(new HashMap<>(requestRep.getAttributes()));

        return requestModel;
    }

    @Override
    public boolean approveRequest(String requestId, RealmModel realm) {
        return processRequest(requestId, realm, true);
    }

    @Override
    public boolean rejectRequest(String requestId, RealmModel realm) {
        return processRequest(requestId, realm, false);
    }

    private boolean processRequest(String requestId, RealmModel realm, boolean approve) {
        ApprovalRequestStore store = getRequestStore();
        ApprovalRequestModel requestModel = store.getRequestById(requestId, realm);
        if (requestModel == null) {
            return false;
        }
        ApprovalHandler handler = getHandlerByRequest(requestModel);

        boolean tx = false;
        KeycloakTransactionManager tm = session.getTransactionManager(); // TODO Is that necessary?
        if (!tm.isActive()) {
            tx = true;
            tm.begin();
        }

        if (approve) {
            handler.handleRequestApproval(requestModel);
        }
        else {
            handler.handleRequestRejection(requestModel);
        }

        for (ApprovalListener listener : session.getAllProviders(ApprovalListener.class)) { // TODO Priorities, enabling?
            if (approve) {
                listener.afterRequestApproval(requestModel);
            }
            else {
                listener.afterRequestRejection(requestModel);
            }
        }

        if (!store.removeRequest(requestModel)) {
            throw new IllegalStateException("Approval request is already removed");
        }

        if (tx) {
            tm.commit();
        }

        return true;
    }

    @Override
    public void close() {

    }
}
