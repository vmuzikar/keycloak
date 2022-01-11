/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a set of resource names that are related to (but not owned by) some CR. To be used by a controller for watching
 * those resources. Also stores a {@code modified} flag for each resource. Motivation for this in-memory store is to avoid
 * touching (e.g. adding labels) the resources that are not owned by the operator and not to store state in K8s.
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WatchedResourcesStore {
    // namespace --> CR name --> resource name --> modified?
    Map<String, Map<String, Map<String, Boolean>>> resources = new HashMap<>();

    /**
     * Sets the flag to indicate whether given resource has been modified.
     *
     * @param crNamespace
     * @param crName
     * @param resourceName
     * @param modified
     */
    public void setResourceModified(String crNamespace, String crName, String resourceName, boolean modified) {
        resources.get(crNamespace).get(crName).put(resourceName, modified);
    }

    /**
     * Set the modified flag to {@code false} for all resources that are related to given CR.
     * 
     * @param crNamespace
     * @param crName
     * @return true if there were some modified resources, otherwise false
     */
    public boolean resetModified(String crNamespace, String crName) {
        boolean foundModified = false;

        for (var resource : getResources(crNamespace, crName).entrySet()) {
            if (resource.setValue(false)) {
                foundModified = true;
            }
        }

        return foundModified;
    }

    /**
     * Sets all resource names that are related to given CR. This is basically a merge that does not touch (particularly
     * the modified flag) any existing resources.
     *
     * @param namespace
     * @param crName
     * @param resourcesNames
     */
    public void setResourcesForCr(String namespace, String crName, Set<String> resourcesNames) {
        resources.putIfAbsent(namespace, new HashMap<>());
        resources.get(namespace).putIfAbsent(crName, new HashMap<>());

        resources.get(namespace).put(crName, resourcesNames.stream().collect(Collectors.toMap(s -> s,
                // preserve old value
                s -> Objects.requireNonNullElse(resources.get(namespace).get(crName).get(s), true)
        )));
    }

    /**
     * Gets all CR names to which the resourceName is related to.
     *
     * @param resourceName
     * @return
     */
    public Set<String> getCRNamesForResource(String resourceName) {
        Set<String> ret = new HashSet<>();

        for (var crs : resources.entrySet()) {
            for (var resources : crs.getValue().entrySet()) {
                var resource = resources.getValue();
                if (resource.containsKey(resourceName)) {
                    ret.add(resources.getKey());
                }
            }
        }

        return ret;
    }

    /**
     * Gets all resource names that are related to given CR.
     *
     * @param crNamespace
     * @param crName
     * @return
     */
    public Set<String> getWatchedResources(String crNamespace, String crName) {
        return Collections.unmodifiableSet(getResources(crNamespace, crName).keySet());
    }

    /**
     * Removes all related resources for given CR.
     *
     * @param crNamespace
     * @param crName
     */
    public void removeResourcesForCR(String crNamespace, String crName) {
        var resourcesInNamespace = resources.get(crNamespace);
        if (resourcesInNamespace == null) {
            return;
        }

        resourcesInNamespace.remove(crName);
    }

    private Map<String, Boolean> getResources(String crNamespace, String crName) {
        var crsInNamespace = resources.get(crNamespace);
        if (crsInNamespace == null) {
            return Collections.emptyMap();
        }

        var resourcesForCr = crsInNamespace.get(crName);
        if (resourcesForCr == null) {
            return Collections.emptyMap();
        }

        return resourcesForCr;
    }
}
