/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProvider implements InfinispanConnectionProvider {

    private final EmbeddedCacheManager cacheManager;
    private final String siteName;
    private final String nodeName;

    public DefaultInfinispanConnectionProvider(EmbeddedCacheManager cacheManager, String nodeName, String siteName) {
        this.cacheManager = cacheManager;
        this.nodeName = nodeName;
        this.siteName = siteName;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        return cacheManager.getCache(name);
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public void close() {
    }

}
