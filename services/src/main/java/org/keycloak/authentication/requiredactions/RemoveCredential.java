/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;

import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RemoveCredential implements RequiredActionProvider, RequiredActionFactory {
    public static final String PROVIDER_ID = "remove_credential";

    public static final String CREDENTIAL_ID_TO_BE_REMOVED = "credential_id_to_be_removed";
    public static final String CREDENTIAL_ID = "credential_id";
    public static final String CREDENTIAL_LABEL = "credential_label";

    @Override
    public String getDisplayText() {
        return "Remove Credential";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        final String credentialId = getCredentialId(context);
        if (credentialId == null) {
            context.form().setError(Messages.NO_CREDENTIAL_ID_SPECIFIED).createErrorPage(Response.Status.BAD_REQUEST);
            return;
        }

        final CredentialModel credential = context.getSession().userCredentialManager().getStoredCredentialById(context.getRealm(), context.getUser(), credentialId);
        if (credential == null) {
            context.form().setError(Messages.CREDENTIAL_ID_NOT_FOUND).createErrorPage(Response.Status.BAD_REQUEST);
            return;
        }

        context.form().setAttribute(CREDENTIAL_ID, credentialId);
        context.form().setAttribute(CREDENTIAL_LABEL, credential.getUserLabel());

        context.challenge(context.form().createForm("remove-credential.ftl"));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        final String credentialId = context.getHttpRequest().getDecodedFormParameters().getFirst(CREDENTIAL_ID);
        if (!context.getSession().userCredentialManager().removeStoredCredential(context.getRealm(), context.getUser(), credentialId)) {
            context.form().setError(Messages.CREDENTIAL_ID_NOT_FOUND).createErrorPage(Response.Status.BAD_REQUEST);
            return;
        }
        context.success();
    }

    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private String getCredentialId(RequiredActionContext context) {
        if (isTriggeredFromAIA(context)) {
            return context.getAuthenticationSession().getAuthNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + CREDENTIAL_ID_TO_BE_REMOVED);
        }
        else {
            return context.getUser().getFirstAttribute(CREDENTIAL_ID_TO_BE_REMOVED);
        }
    }

    private boolean isTriggeredFromAIA(RequiredActionContext context) {
        return PROVIDER_ID.equals(context.getAuthenticationSession().getClientNote(Constants.KC_ACTION));
    }
}
