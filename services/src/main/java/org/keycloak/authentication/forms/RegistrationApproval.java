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

package org.keycloak.authentication.forms;

import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.ApprovalInterceptor;
import org.keycloak.approvals.InterceptedException;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RegistrationApproval implements Authenticator {
    public void authenticate(AuthenticationFlowContext context) {
        final KeycloakSession session = context.getSession();
        final ApprovalInterceptor approval = session.getProvider(ApprovalInterceptor.class);
        final UserModel userModel = context.getUser();

        try {
            approval.intercept(ApprovalContext.fromModel(userModel, context.getRealm()));
        }
        catch (InterceptedException e) {
            context.challenge(context.form().createRegisterApprovalNeededPage());
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
