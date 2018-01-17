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

import org.keycloak.approvals.evaluators.RoleEvaluator;
import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.approvals.store.ApprovalStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalManager implements ApprovalManager {
    protected KeycloakSession session;

    public DefaultApprovalManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ApprovalStore getStore() {
        return session.getProvider(ApprovalStore.class); // TODO caching layer...
    }

    @Override
    public ApprovalHandler getHandlerByRequest(String requestId, RealmModel realmModel) {
        ApprovalRequestModel request = getStore().getRequestById(requestId, realmModel);

        if (request == null) {
            return null;
        }

        return getHandlerByRequest(request);
    }

    @Override
    public ApprovalHandler getHandlerByRequest(ApprovalRequestModel requestModel) {
        return session.getProvider(ApprovalHandler.class, requestModel.getHandlerId());
    }

    protected ApprovalEvaluator getEvaluator(ApprovalContext context) {
        return session.getProvider(ApprovalEvaluator.class, RoleEvaluator.PROVIDER_ID);
    }

    @Override
    public void interceptAction(ApprovalContext context) throws InterceptedException {
        ApprovalHandler handler = session.getProvider(ApprovalHandler.class, context.getAction().getHandlerId());

        if (getEvaluator(context).needsApproval(context)) {
            boolean tx = false;
            KeycloakTransactionManager tm = session.getTransactionManager(); // TODO Is that necessary?
            if (!tm.isActive()) {
                tx = true;
                tm.begin();
            }

            ApprovalRequestRepresentation requestRep = handler.handleRequestCreation(context);
            ApprovalRequestModel requestModel = createRequest(requestRep, context.getRealm());

            for (ApprovalListener listener : getEnabledListeners(context.getRealm())) {
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
        ApprovalRequestModel requestModel = getStore().createRequest(realm, requestRep.getHandlerId());

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
        ApprovalStore store = getStore();
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

        for (ApprovalListener listener : getEnabledListeners(realm)) {
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
    public List<ApprovalListener> getEnabledListeners(RealmModel realm) {
        List<ApprovalListener> listeners = new ArrayList<>();

        for (ProviderFactory providerFactory : session.getKeycloakSessionFactory().getProviderFactories(ApprovalListener.class)) {
            String providerId = providerFactory.getId();
            ApprovalListenerConfigModel config = getStore().createOrGetListenerConfig(providerId, realm);
            if (config.isEnabled()) {
                ApprovalListener listener = session.getProvider(ApprovalListener.class, providerId);
                listener.setConfig(config);
                listeners.add(listener);
            }
        }

        return listeners;
    }

    @Override
    public void close() {

    }
}
