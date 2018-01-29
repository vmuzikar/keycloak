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
import org.keycloak.approvals.ApprovalManager;
import org.keycloak.approvals.store.ApprovalListenerConfigModel;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ApprovalRequestBPMSRepresentation;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class BPMSListener implements ApprovalListener {
    public static final String PROVIDER_ID = "bpms";

    public static final String CONTAINER_ID = "containerId";
    public static final String PROCESS_ID = "processId";
    public static final String SERVER_URL = "serverUrl";
    public static final String LOGIN = "login";
    public static final String PASSWORD = "password";
    public static final String BPMS_PROCESS_INSTANCE_ID = "bpmsProcessId";

    private final Logger log = Logger.getLogger(this.getClass());
    private KeycloakSession session;
    private ApprovalListenerConfigModel config = null;

    public BPMSListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void afterRequestCreation(ApprovalRequestModel request, ApprovalContext context) {
        KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(getConfigValue(SERVER_URL), getConfigValue(LOGIN), getConfigValue(PASSWORD));

        conf.addExtraClasses(Collections.singleton(ApprovalRequestBPMSRepresentation.class));
        conf.setMarshallingFormat(MarshallingFormat.JSON);
        KieServicesClient kieClient = KieServicesFactory.newKieServicesClient(conf);

        log.infov("Connection with KIE server established: {0}", kieClient.getServerInfo());

        ProcessServicesClient processServicesClient = kieClient.getServicesClient(ProcessServicesClient.class);

        Map<String, Object> args = new HashMap<>();
        args.put("approvalRequestRep", ModelToRepresentation.toBPMSRepresentation(session, request));

        String containerId = getConfigValue(CONTAINER_ID);
        String processId = getConfigValue(PROCESS_ID);
        long id = processServicesClient.startProcess(containerId, processId, args);
        log.infov("Process started; contained id: {0}; process id: {1}; instance id: {2}", containerId, processId, id);
    }

    @Override
    public void afterRequestApproval(ApprovalRequestModel request) {

    }

    @Override
    public void afterRequestRejection(ApprovalRequestModel request) {

    }

    @Override
    public ApprovalListenerConfigModel getConfig() {
        if (config == null) {
            config = session.getProvider(ApprovalManager.class).getStore().createOrGetListenerConfig(PROVIDER_ID, session.getContext().getRealm());
        }
        return config;
    }

    @Override
    public void setConfig(ApprovalListenerConfigModel config) {
        this.config = config;
    }

    protected String getConfigValue(String key) {
        return getConfig().getConfigs().get(key);
    }

    @Override
    public void close() {

    }
}
