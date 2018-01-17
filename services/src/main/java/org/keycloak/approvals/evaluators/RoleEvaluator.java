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

package org.keycloak.approvals.evaluators;

import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalEvaluator;
import org.keycloak.approvals.store.EvaluatorStore;
import org.keycloak.models.KeycloakSession;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RoleEvaluator implements ApprovalEvaluator {
    public static final String PROVIDER_ID = "role";

    protected KeycloakSession session;

    public RoleEvaluator(KeycloakSession session) {
        this.session = session;
    }

    public EvaluatorStore getEvaluatorStore() {
        return session.getProvider(EvaluatorStore.class); // TODO caching layer...
    }

    @Override
    public boolean needsApproval(ApprovalContext context) {
        return true; // TODO make it dynamic!!!
    }

    @Override
    public void close() {

    }
}
