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

import com.google.common.collect.ImmutableSet;
import org.keycloak.approvals.ApprovalContext;
import org.keycloak.authentication.forms.RegistrationApproval;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersHandler extends AbstractApprovalHandler {
    @Override
    public Set<Class> getProtectedClasses() {
        return ImmutableSet.of(UsersResource.class, UserResource.class, RegistrationApproval.class);
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
}
