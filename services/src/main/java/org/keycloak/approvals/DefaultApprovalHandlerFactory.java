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

package org.keycloak.approvals;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalHandlerFactory implements ApprovalHandlerFactory {
    private Map<Class, Class<? extends ApprovalHandler>> handlers = new HashMap<>();

    @Override
    public ApprovalHandler create(KeycloakSession session) {
        throw new UnsupportedOperationException("You need to specify the protected class for the handler");
    }

    @Override
    public ApprovalHandler create(KeycloakSession session, Class protectedClass) {
        try {
            return handlers.get(protectedClass).newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(Config.Scope config) {
        for (ApprovalHandler handler : ServiceLoader.load(ApprovalHandler.class)) {
            for (Class protectedClass : handler.getProtectedClasses()) {
                if (handlers.putIfAbsent(protectedClass, handler.getClass()) != null) {
                    throw new IllegalStateException("Multiple handlers found for " + protectedClass);
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }
}
