/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApprovalContext {
    public static final String REPRESENTANTION_ATTR = "representantion";

    private Map<String, Object> attributes = new HashMap<>();

    public static ApprovalContext fromRep(Object representation) {
        return (new ApprovalContext()).setAttribute(REPRESENTANTION_ATTR, representation);
    }

    public ApprovalContext setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Object getAttribute(String name) {
        Object value = attributes.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Attribute '" + name + "' not found!");
        }
        return value;
    }

    public Object getRepresentation() {
        return getAttribute(REPRESENTANTION_ATTR);
    }
}
