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

import org.keycloak.approvals.ApprovalAction;
import org.keycloak.approvals.ApprovalContext;
import org.keycloak.approvals.store.ApprovalRequestModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ApprovalRequestRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersHandler extends AbstractApprovalHandler {
    public enum Actions implements ApprovalAction {
        REGISTER_USER("User self-registers using login page"),
        CREATE_USER("User is created using Admin Console");

        private String description;

        Actions(String description) {
            this.description = description;
        }

        @Override
        public String getHandlerId() {
            return HANDLER_ID;
        }

        @Override
        public String getActionId() {
            return name();
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public static final String HANDLER_ID = "users";

    public static ApprovalContext createUserCtx(UserRepresentation rep, RealmModel realm) {
        return ApprovalContext.withRepresentation(realm, Actions.CREATE_USER, rep);
    }

    public static ApprovalContext registerUserCtx(UserModel user, RealmModel realm) {
        return ApprovalContext.withModel(realm, Actions.REGISTER_USER, user);
    }

    public UsersHandler(KeycloakSession session) {
        super(session);
    }

    @Override
    public ApprovalRequestRepresentation handleRequestCreation(ApprovalContext context) {
        switch ((Actions)context.getAction()) {
            case REGISTER_USER:
                return prepareUserRegistration(context);
            case CREATE_USER:
                return contextToRepresentation(context);
        }

        return null;
    }

    @Override
    public void handleRequestApproval(ApprovalRequestModel request) {
        Actions action = Actions.valueOf(request.getActionId().toUpperCase());

        switch (action) {
            case REGISTER_USER:
                performUserRegistration(request);
            case CREATE_USER:

        }
    }

    @Override
    public void handleRequestRejection(ApprovalRequestModel request) {

    }

    private ApprovalRequestRepresentation prepareUserRegistration(ApprovalContext context) {
        UserModel userModel = (UserModel)context.getModel();

        userModel.setEnabled(false);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(userModel.getId());
        userRep.setEnabled(true);

        context.setRepresentation(userRep);

        String desc;
        try {
            desc = JsonSerialization.writeValueAsPrettyString(ModelToRepresentation.toRepresentation(session, context.getRealm(), userModel));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return contextToRepresentation(context, desc);
    }

    private void performUserRegistration(ApprovalRequestModel request) {
        UserRepresentation userRep;

        try {
            userRep = JsonSerialization.readValue(
                    request.getAttribute(ApprovalContext.REPRESENTATION_ATTR),
                    UserRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException();
        }

        UserModel userModel = session.users().getUserById(userRep.getId(), request.getRealm());
        UserResource.updateUserFromRep(userModel, userRep, null, request.getRealm(), session, false);
    }

    @Override
    public ApprovalAction[] getSupportedActions() {
        return Actions.values();
    }

    @Override
    public ApprovalAction getActionById(String id) {
        return Actions.valueOf(id);
    }
}
