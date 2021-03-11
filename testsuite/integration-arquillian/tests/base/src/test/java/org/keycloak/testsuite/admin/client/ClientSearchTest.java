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

package org.keycloak.testsuite.admin.client;

import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientSearchTest extends AbstractClientTest {
    @Test
    public void testQuerySearch() {
        ClientRepresentation client1 = createOidcClientRep("client1");
        ClientRepresentation client2 = createOidcClientRep("client2");
        ClientRepresentation client3 = createOidcClientRep("client3");

        client1.setAttributes(new HashMap<String, String>() {{
            put("attr1", "val1");
            put("attr2", "val2");
        }});

        client2.setAttributes(new HashMap<String, String>() {{
            put("attr2", "val2");
        }});

        client3.setAttributes(new HashMap<String, String>() {{
            put("attr1", "val2");
        }});

        createClient(client1);
        createClient(client2);
        createClient(client3);

        search("attr1:val1", "client1");
        search("attr2:val2", "client1", "client2");
        search("attr1:val1+attr2:val2", "client1");
        search("attr1:wrongval+attr2:val2");
        search("attr1");
        search("val1");
        search("-attr1:val1"); // negative search not yet supported
    }

    private void search(String searchQuery, String... expectedClientIds) {
        List<String> found = testRealmResource().clients().query(searchQuery).stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toList());
        assertThat(found, containsInAnyOrder(expectedClientIds));
    }
}
