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

package org.keycloak.operator.v2alpha1;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.quarkus.logging.Log;
import org.keycloak.operator.Constants;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WatchedResourcesStore<T extends HasMetadata> extends OperatorManagedResource {
    public static final String STORE_LABEL = Constants.CRDS_GROUP + "/watched-resources-store";
    public static final String WATCHED_LABEL = Constants.CRDS_GROUP + "/watched-by";

    private final Class<T> clazz;
    private final Keycloak keycloakCR;
    private final Set<WatchableResource> watched;
    private final Secret storeK8sResource;
    private final Map<String, String> store;

    public WatchedResourcesStore(Class<T> clazz, Set<String> desiredWatchedNames, KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
        this.clazz = clazz;
        this.keycloakCR = keycloakCR;

        // first load previous K8s representation of the store
        Log.infof("Trying to fetch existing store \"%s\" from the API", getName());
        Secret storeK8sResource = getExistingStoreK8sResource();
        if (storeK8sResource == null) {
            Log.info("Existing store not found");
            storeK8sResource = getDefaultStoreK8sResource();
        }
        this.storeK8sResource = storeK8sResource;

        // translate it to map based on the new desired watched resources
        this.store = getNewStore(desiredWatchedNames);

        // based on the current store, load watched resources
        this.watched = getWatchedResources();

        Log.debugf("Watched Resources: type %s, content %s", HasMetadata.getPlural(clazz).toLowerCase(), store);
    }

    public boolean requiresRestart() {
        return store.entrySet().stream()
                .anyMatch(e -> e.getValue() == null || !e.getValue().equals(getCurrentWatchedResourceVersion(e.getKey())));
    }

    private Map<String, String> getNewStore(Set<String> resourcesNames) {
        resourcesNames = Objects.requireNonNullElse(resourcesNames, Collections.emptySet());
        Map<String, String> ret = new HashMap<>();
        for (var name : resourcesNames) {
            String version = null;
             if (storeK8sResource.getData() != null && storeK8sResource.getData().get(name) != null) {
                 version = new String(Base64.getDecoder().decode(storeK8sResource.getData().get(name)));
             }
             ret.put(name, version);
        }
        return ret;
    }

    private Secret getDefaultStoreK8sResource() {
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withNamespace(getNamespace())
                .endMetadata()
                .build();
    }

    private Secret getExistingStoreK8sResource() {
        return client.secrets()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    @Override
    protected HasMetadata getReconciledResource() {
        if (storeK8sResource.getMetadata().getLabels() == null) {
            storeK8sResource.getMetadata().setLabels(new HashMap<>());
        }

        storeK8sResource.getMetadata().getLabels().put(STORE_LABEL, HasMetadata.getPlural(clazz).toLowerCase());

        Map<String, String> data = new HashMap<>(store);
        for (var res : data.entrySet()) {
            String version = getCurrentWatchedResourceVersion(res.getKey());
            res.setValue(version);
        }

        storeK8sResource.setStringData(data);

        return storeK8sResource;
    }

    public void reconcileWatchedResourcesLabels() {
        watched.forEach(WatchableResource::persist);
    }

    public String getName() {
        return keycloakCR.getMetadata().getName() + "-watched-" + HasMetadata.getPlural(clazz).toLowerCase();
    }

    public String getNamespace() {
        return keycloakCR.getMetadata().getNamespace();
    }

    public String getCurrentWatchedResourceVersion(String name) {
        return watched.stream()
                .filter(r -> r.getName().equals(name))
                .map(WatchableResource::getVersion)
                .findFirst()
                .orElse(null);
    }

    private Set<WatchableResource> getWatchedResources() {
        Set<WatchableResource> watched = client.resources(clazz)
                .inNamespace(getNamespace())
                .withLabel(WATCHED_LABEL)
                .list()
                .getItems()
                .stream()
                .map(WatchableResource::new)
                .collect(Collectors.toSet());

        var desiredWatchedNames = store.keySet();

        // check removed resources to remove the CR from its annotation
        for (var watchedResource : watched) {
            if (!desiredWatchedNames.contains(watchedResource.getName())) {
                watchedResource.unwatch();
            }
        }

        // check added resources to add the CR to its annotation
        for (var watchedDesiredResource : desiredWatchedNames) {
            if (watched.stream().noneMatch(r -> r.getName().equals(watchedDesiredResource))) {
                T resourceRep = client.resources(clazz)
                        .inNamespace(getNamespace())
                        .withName(watchedDesiredResource)
                        .get();
                if (resourceRep == null) {
                    throw new IllegalStateException("Resource " + watchedDesiredResource + " not found");
                }
                WatchableResource watchableResource = new WatchableResource(resourceRep);
                watchableResource.watch();
                watched.add(watchableResource);
            }
        }

        return watched;
    }

    private class WatchableResource {
        private final T watchedResource;
        private final Set<String> watchedByCRs;
        private boolean modified = false;

        public WatchableResource(T watchedResource) {
            this.watchedResource = watchedResource;
            this.watchedByCRs = getWatchedCRNames(watchedResource);
        }

        public String getName() {
            return watchedResource.getMetadata().getName();
        }

        public void watch() {
            Log.debugf("Start watching resource %s", getName());
            watchedByCRs.add(keycloakCR.getMetadata().getName());
            modified = true;
        }

        public void unwatch() {
            Log.debugf("Stop watching resource %s", getName());
            watchedByCRs.remove(keycloakCR.getMetadata().getName());
            modified = true;
        }

        public void persist() {
            if (modified) {
                if (watchedResource.getMetadata().getLabels() == null) {
                    watchedResource.getMetadata().setLabels(new HashMap<>());
                }

                if (!watchedByCRs.isEmpty()) {
                    watchedResource.getMetadata().getLabels().put(WATCHED_LABEL, String.join(",", watchedByCRs));
                } else {
                    watchedResource.getMetadata().getLabels().remove(WATCHED_LABEL);
                }

                Log.debugf("Persisting: %s, labels: %s", watchedResource.getMetadata().getName(), watchedResource.getMetadata().getLabels());
                client.resource(watchedResource).createOrReplace();
                modified = false;
            }
        }

        public String getVersion() {
            return watchedResource.getMetadata().getResourceVersion();
        }
    }

    public static Set<String> getWatchedCRNames(HasMetadata resource) {
        Set<String> ret = new HashSet<>();
        if (resource.getMetadata() != null
                && resource.getMetadata().getLabels() != null
                && resource.getMetadata().getLabels().get(WATCHED_LABEL) != null) {
            Collections.addAll(ret, resource.getMetadata().getLabels().get(WATCHED_LABEL).split(","));
        }
        return ret;
    }

    public static <T extends HasMetadata> EventSource getStoreEventSource(KubernetesClient client, Class<T> clazz) {
        SharedIndexInformer<Secret> informer =
                client.secrets()
                        .inAnyNamespace()
                        .withLabel(STORE_LABEL)
                        .runnableInformer(0);

        return new InformerEventSource<>(informer, s -> {
            String suffix = "-watched-" + HasMetadata.getPlural(clazz).toLowerCase();
            return Collections.singleton(new ResourceID(s.getMetadata().getName().split(suffix)[0], s.getMetadata().getNamespace()));
        }) {
            @Override
            public String name() {
                return "watchedResourcesStoreEventSource";
            }
        };
    }

    public static <T extends HasMetadata> EventSource getWatchedResourcesEventSource(KubernetesClient client, Class<T> clazz) {
        SharedIndexInformer<T> informer =
                client.resources(clazz)
                        .inAnyNamespace()
                        .withLabel(WATCHED_LABEL)
                        .runnableInformer(0);

        return new InformerEventSource<>(informer, r -> getWatchedCRNames(r).stream()
                    .map(n -> new ResourceID(n, r.getMetadata().getNamespace()))
                    .collect(Collectors.toSet())) {
            @Override
            public String name() {
                return "watchedResourcesEventSource";
            }
        };
    }
}
