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

import org.jboss.logging.Logger;
import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalListener;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.models.KeycloakSession;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class BPMSListener implements ApprovalListener {
    private final Logger log = Logger.getLogger(this.getClass());
    private KeycloakSession session;

    public BPMSListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void afterRequestCreation(ApprovalRequestModel request, ApprovalContext context) {
        ProcessServicesClient processServicesClient = getProcessServicesClient();

        Map<String, Object> args = new HashMap<>();
        args.put("keycloakRootUrl", session.getContext().getAuthServerUrl());
        args.put("realm", request.getRealm().getId());
        args.put("approvalId", request.getId());
        args.put("name", context.getAction().getDescription());
        args.put("description", request.getDescription());

        processServicesClient.startProcess("org.keycloak.quickstart:bpm:1.0", "bpm-quickstart.HandleApprovalRequest", args);
    }

    @Override
    public void afterRequestApproval(ApprovalRequestModel request) {

    }

    @Override
    public void afterRequestRejection(ApprovalRequestModel request) {

    }

    private ProcessServicesClient getProcessServicesClient() {
        KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration("http://localhost:8080/kie-server/services/rest/server", "kieuser", "BPMpassword1;");

        conf.setMarshallingFormat(MarshallingFormat.JSON);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

        log.infov("Connection with KIE server established: {0}", kieServicesClient.getServerInfo());

        return kieServicesClient.getServicesClient(ProcessServicesClient.class);
    }

    @Override
    public void close() {

    }
}
