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

package org.keycloak.approvals.handlers;

import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.authentication.forms.RegistrationApproval;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersHandler extends AbstractApprovalHandler {
    private static final Class[] protectedClasses = new Class[] {UsersResource.class, UserResource.class, RegistrationApproval.class};

    @Override
    public Class[] getProtectedClasses() {
        return protectedClasses;
    }

    @Override
    public void handleRequest(Method protectedMethod, ApprovalContext context) {
        if (protectedMethod.getDeclaringClass().equals(RegistrationApproval.class)) {
            UserModel userModel = (UserModel)context.getModel();

            userModel.setEnabled(false);

            UserRepresentation userRep = new UserRepresentation();
            userRep.setId(userModel.getId());
            userRep.setEnabled(true);

            context = ApprovalContext.fromRep(userRep, context.getRealm());
        }

        super.handleRequest(protectedMethod, context);
    }

    @Override
    protected void executeRequestedActions(ApprovalRequestModel request) {
        if (RegistrationApproval.class.getName().equals(request.getRequester())) {
            handleUserRegistration(request);
        }
    }

    private void handleUserRegistration(ApprovalRequestModel request) {
        UserRepresentation userRep;

        try {
            userRep = JsonSerialization.readValue(
                    request.getAttribute(ApprovalContext.REPRESENTATION_ATTR),
                    UserRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException();
        }

        session.users().getUserById(userRep.getId(), request.getRealm()).setEnabled(true);
    }
}
