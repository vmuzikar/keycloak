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

package org.keycloak.testsuite.admin.realm;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RoleBuilder;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.Assert.assertNames;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmRolesTest extends AbstractAdminTest {

    private RolesResource resource;

    private Map<String, String> ids = new HashMap<>();
    private String clientUuid;

    @Before
    public void before() {
        RoleRepresentation roleA = RoleBuilder.create().name("role-a").description("Role A").build();
        RoleRepresentation roleB = RoleBuilder.create().name("role-b").description("Role B").build();
        //KEYCLOAK-2035
        RoleRepresentation roleWithUsers = RoleBuilder.create().name("role-with-users").description("Role with users").build();
        RoleRepresentation roleWithoutUsers = RoleBuilder.create().name("role-without-users").description("role-without-users").build();
        adminClient.realm(REALM_NAME).roles().create(roleA);
        adminClient.realm(REALM_NAME).roles().create(roleB);
        adminClient.realm(REALM_NAME).roles().create(roleWithUsers);
        adminClient.realm(REALM_NAME).roles().create(roleWithoutUsers);

        
        ClientRepresentation clientRep = ClientBuilder.create().clientId("client-a").build();
        Response response = adminClient.realm(REALM_NAME).clients().create(clientRep);
        clientUuid = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(clientUuid);
        response.close();

        RoleRepresentation roleC = RoleBuilder.create().name("role-c").description("Role C").build();
        adminClient.realm(REALM_NAME).clients().get(clientUuid).roles().create(roleC);

        for (RoleRepresentation r : adminClient.realm(REALM_NAME).roles().list()) {
            ids.put(r.getName(), r.getId());
        }

        for (RoleRepresentation r : adminClient.realm(REALM_NAME).clients().get(clientUuid).roles().list()) {
            ids.put(r.getName(), r.getId());
        }
        
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("test-role-member");
        userRep.setEmail("test-role-member@test-role-member.com");
        userRep.setRequiredActions(Collections.<String>emptyList());
        userRep.setEnabled(true);        
        adminClient.realm(REALM_NAME).users().create(userRep);

        getCleanup().addRoleId(ids.get("role-a"));
        getCleanup().addRoleId(ids.get("role-b"));
        getCleanup().addRoleId(ids.get("role-c"));
        getCleanup().addRoleId(ids.get("role-with-users"));
        getCleanup().addRoleId(ids.get("role-without-users"));
        getCleanup().addUserId(adminClient.realm(REALM_NAME).users().search(userRep.getUsername()).get(0).getId());
        

        resource = adminClient.realm(REALM_NAME).roles();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("role-a"), roleA, ResourceType.REALM_ROLE);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("role-b"), roleB, ResourceType.REALM_ROLE);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("role-with-users"), roleWithUsers, ResourceType.REALM_ROLE);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("role-without-users"), roleWithoutUsers, ResourceType.REALM_ROLE);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(clientUuid), clientRep, ResourceType.CLIENT);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientUuid, "role-c"), roleC, ResourceType.CLIENT_ROLE);
        
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(adminClient.realm(REALM_NAME).users().search(userRep.getUsername()).get(0).getId()), userRep, ResourceType.USER);
        
        
    }

    @Test
    public void getRole() {
        RoleRepresentation role = resource.get("role-a").toRepresentation();
        assertNotNull(role);
        assertEquals("role-a", role.getName());
        assertEquals("Role A", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void updateRole() {
        RoleRepresentation role = resource.get("role-a").toRepresentation();

        role.setName("role-a-new");
        role.setDescription("Role A New");

        resource.get("role-a").update(role);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.roleResourcePath("role-a"), role, ResourceType.REALM_ROLE);

        role = resource.get("role-a-new").toRepresentation();

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    public void deleteRole() {
        assertNotNull(resource.get("role-a"));
        resource.deleteRole("role-a");
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleResourcePath("role-a"), ResourceType.REALM_ROLE);

        try {
            resource.get("role-a").toRepresentation();
            fail("Expected 404");
        } catch (NotFoundException e) {
            // expected
        }
    }

    @Test
    public void composites() {
        assertFalse(resource.get("role-a").toRepresentation().isComposite());
        assertEquals(0, resource.get("role-a").getRoleComposites().size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleBuilder.create().id(ids.get("role-b")).build());
        l.add(RoleBuilder.create().id(ids.get("role-c")).build());
        resource.get("role-a").addComposites(l);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = resource.get("role-a").getRoleComposites();

        assertTrue(resource.get("role-a").toRepresentation().isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = resource.get("role-a").getRealmRoleComposites();
        Assert.assertNames(realmComposites, "role-b");

        Set<RoleRepresentation> clientComposites = resource.get("role-a").getClientRoleComposites(clientUuid);
        Assert.assertNames(clientComposites, "role-c");

        resource.get("role-a").deleteComposites(l);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        assertFalse(resource.get("role-a").toRepresentation().isComposite());
        assertEquals(0, resource.get("role-a").getRoleComposites().size());
    }

    /**
     * KEYCLOAK-2035 Verifies that Users assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testUsersInRole() {   
        RoleResource role = resource.get("role-with-users");

        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search("test-role-member", null, null, null, null, null);
        assertEquals(1, users.size());
        UserResource user = adminClient.realm(REALM_NAME).users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = adminClient.realm(REALM_NAME).roles().get(role.toRepresentation().getName());        
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        adminClient.realm(REALM_NAME).users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = adminClient.realm(REALM_NAME).roles().get(role.toRepresentation().getName());  
        roleResource.getRoleUserMembers();
        //roleResource.getRoleUserMembers().stream().forEach((member) -> log.infof("Found user {}", member.getUsername()));
        assertEquals(1, roleResource.getRoleUserMembers().size());

    }
    
    /**
     * KEYCLOAK-2035  Verifies that Role with no users assigned is being properly retrieved without members in API endpoint for role membership
     */
    @Test
    public void testUsersNotInRole() {
        RoleResource role = resource.get("role-without-users");                
        
        role = adminClient.realm(REALM_NAME).roles().get(role.toRepresentation().getName());
        role.getRoleUserMembers();
        assertEquals(0, role.getRoleUserMembers().size());
        
    }
    
    /**
     * KEYCLOAK-2035 Verifies that Role Membership is ok after user removal
     */
    @Test
    public void roleMembershipAfterUserRemoval() {    
        RoleResource role = resource.get("role-with-users");

        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search("test-role-member", null, null, null, null, null);
        assertEquals(1, users.size());
        UserResource user = adminClient.realm(REALM_NAME).users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = adminClient.realm(REALM_NAME).roles().get(role.toRepresentation().getName());        
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        adminClient.realm(REALM_NAME).users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = adminClient.realm(REALM_NAME).roles().get(role.toRepresentation().getName());  
        roleResource.getRoleUserMembers();
        assertEquals(1, roleResource.getRoleUserMembers().size());

        adminClient.realm(REALM_NAME).users().delete(userRep.getId());
        roleResource.getRoleUserMembers();
        assertEquals(0, roleResource.getRoleUserMembers().size());

    }
    
}
