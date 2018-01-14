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

package org.keycloak.approvals.listeners;

import org.keycloak.Config;
import org.keycloak.approvals.ApprovalListener;
import org.keycloak.approvals.ApprovalListenerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class BPMSListenerFactory implements ApprovalListenerFactory {
    @Override
    public ApprovalListener create(KeycloakSession session) {
        return new BPMSListener(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return BPMSListener.PROVIDER_ID;
    }

    @Override
    public boolean enabledByDefault() {
        return true; // TODO change this !!!
    }

    @Override
    public Map<String, String> getDefaultConfigs() {
        // TODO change this !!!
        Map<String, String> defaults = new HashMap<>();
        defaults.put(BPMSListener.CONTAINER_ID, "org.keycloak.quickstart:bpm:1.0");
        defaults.put(BPMSListener.PROCESS_ID, "bpm-quickstart.HandleApprovalRequest");
        defaults.put(BPMSListener.SERVER_URL, "http://localhost:8080/kie-server/services/rest/server");
        defaults.put(BPMSListener.LOGIN, "kieuser");
        defaults.put(BPMSListener.PASSWORD, "BPMpassword1;");
        return defaults;
    }
}
