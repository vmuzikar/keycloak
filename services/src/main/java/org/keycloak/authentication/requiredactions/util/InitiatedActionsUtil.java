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

package org.keycloak.authentication.requiredactions.util;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class InitiatedActionsUtil {
    /**
     * @param authSession
     * @param session
     * @return max auth age for currently executed AIA, or null if there's no active AIA
     */
    public static Integer getCurrentActionMaxAuthAge(AuthenticationSessionModel authSession, KeycloakSession session) {
        String providerId = authSession.getClientNote(Constants.KC_ACTION);
        if (providerId == null) {
            return null;
        }
        RequiredActionProvider requiredActionProvider = session.getProvider(RequiredActionProvider.class, providerId);
        return requiredActionProvider.getMaxAuthAge();
    }
}
