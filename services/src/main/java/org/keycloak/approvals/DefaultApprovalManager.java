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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;
import org.keycloak.services.managers.AppAuthManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalManager implements ApprovalManager {
    protected KeycloakSession session;
    protected AppAuthManager authManager;

    public DefaultApprovalManager(KeycloakSession session) {
        this.session = session;
        this.authManager = new AppAuthManager();
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
        return session.getProvider(ApprovalHandler.class, requestModel.getAction().getHandlerId());
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
        ApprovalRequestModel requestModel;
        if (session.getContext().getRealm() != null && session.getContext().getAuthUser() != null) {
            requestModel = getStore().createRequest(realm, requestRep.getAction(), session.getContext().getAuthUser(), session.getContext().getAuthRealm());
        }
        else {
            requestModel = getStore().createRequest(realm, requestRep.getAction());
        }

        requestModel.setDescription(requestRep.getDescription());
        requestModel.setAttributes(new HashMap<>(requestRep.getAttributes()));

        return requestModel;
    }

    @Override
    public ApprovalRequestRepresentation approveRequest(String requestId, RealmModel realm) {
        ApprovalRequestModel model = getStore().getRequestById(requestId, realm);
        ApprovalRequestRepresentation rep = ModelToRepresentation.toRepresentation(session, model);
        if (approveRequest(model)) {
            return rep;
        }
        else {
            return null;
        }
    }

    @Override
    public boolean approveRequest(ApprovalRequestModel request) {
        return processRequest(request, true);
    }

    @Override
    public ApprovalRequestRepresentation rejectRequest(String requestId, RealmModel realm) {
        ApprovalRequestModel model = getStore().getRequestById(requestId, realm);
        ApprovalRequestRepresentation rep = ModelToRepresentation.toRepresentation(session, model);
        if (rejectRequest(model)) {
            return rep;
        }
        else {
            return null;
        }
    }

    @Override
    public boolean rejectRequest(ApprovalRequestModel request) {
        return processRequest(request, false);
    }

    private boolean processRequest(ApprovalRequestModel request, boolean approve) {
        if (request == null) {
            return false;
        }
        ApprovalHandler handler = getHandlerByRequest(request);

        boolean tx = false;
        KeycloakTransactionManager tm = session.getTransactionManager(); // TODO Is that necessary?
        if (!tm.isActive()) {
            tx = true;
            tm.begin();
        }

        if (approve) {
            handler.handleRequestApproval(request);
        }
        else {
            handler.handleRequestRejection(request);
        }

        for (ApprovalListener listener : getEnabledListeners(request.getRealm())) {
            if (approve) {
                listener.afterRequestApproval(request);
            }
            else {
                listener.afterRequestRejection(request);
            }
        }

        if (!getStore().removeRequest(request)) {
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
