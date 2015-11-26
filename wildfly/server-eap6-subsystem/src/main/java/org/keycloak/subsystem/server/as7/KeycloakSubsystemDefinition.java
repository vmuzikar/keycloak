/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.server.as7;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of subsystem=keycloak-server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakSubsystemDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition WEB_CONTEXT =
        new SimpleAttributeDefinitionBuilder("web-context", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("auth"))
            .setRestartAllServices()
            .build();

    static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();

    static {
        ALL_ATTRIBUTES.add(WEB_CONTEXT);
    }

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    private static KeycloakSubsystemWriteAttributeHandler attrHandler = new KeycloakSubsystemWriteAttributeHandler(ALL_ATTRIBUTES);

    protected KeycloakSubsystemDefinition() {
        super(KeycloakExtension.PATH_SUBSYSTEM,
            KeycloakExtension.getResourceDescriptionResolver("subsystem"),
            KeycloakSubsystemAdd.INSTANCE,
            KeycloakSubsystemRemoveHandler.INSTANCE
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attrDef : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attrDef, null, attrHandler);
        }
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }
}
