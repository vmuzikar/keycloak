package org.keycloak.exportimport.util;

import org.keycloak.common.util.Base64;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportUtils {

    public static RealmRepresentation exportRealm(KeycloakSession session, RealmModel realm, boolean includeUsers) {
        RealmRepresentation rep = ModelToRepresentation.toRepresentation(realm, true);

        // Clients
        List<ClientModel> clients = realm.getClients();
        List<ClientRepresentation> clientReps = new ArrayList<>();
        for (ClientModel app : clients) {
            ClientRepresentation clientRep = exportClient(app);
            clientReps.add(clientRep);
        }
        rep.setClients(clientReps);

        // Roles
        List<RoleRepresentation> realmRoleReps = null;
        Map<String, List<RoleRepresentation>> clientRolesReps = new HashMap<>();

        Set<RoleModel> realmRoles = realm.getRoles();
        if (realmRoles != null && realmRoles.size() > 0) {
            realmRoleReps = exportRoles(realmRoles);
        }
        for (ClientModel client : clients) {
            Set<RoleModel> currentAppRoles = client.getRoles();
            List<RoleRepresentation> currentAppRoleReps = exportRoles(currentAppRoles);
            clientRolesReps.put(client.getClientId(), currentAppRoleReps);
        }

        RolesRepresentation rolesRep = new RolesRepresentation();
        if (realmRoleReps != null) {
            rolesRep.setRealm(realmRoleReps);
        }
        if (clientRolesReps.size() > 0) {
            rolesRep.setClient(clientRolesReps);
        }
        rep.setRoles(rolesRep);

        // Scopes
        List<ClientModel> allClients = new ArrayList<>(clients);
        Map<String, List<ScopeMappingRepresentation>> clientScopeReps = new HashMap<>();

        for (ClientModel client : allClients) {
            Set<RoleModel> clientScopes = client.getScopeMappings();
            ScopeMappingRepresentation scopeMappingRep = null;
            for (RoleModel scope : clientScopes) {
                if (scope.getContainer() instanceof RealmModel) {
                    if (scopeMappingRep == null) {
                        scopeMappingRep = rep.scopeMapping(client.getClientId());
                    }
                    scopeMappingRep.role(scope.getName());
                } else {
                    ClientModel app = (ClientModel)scope.getContainer();
                    String appName = app.getClientId();
                    List<ScopeMappingRepresentation> currentAppScopes = clientScopeReps.get(appName);
                    if (currentAppScopes == null) {
                        currentAppScopes = new ArrayList<>();
                        clientScopeReps.put(appName, currentAppScopes);
                    }

                    ScopeMappingRepresentation currentClientScope = null;
                    for (ScopeMappingRepresentation scopeMapping : currentAppScopes) {
                        if (scopeMapping.getClient().equals(client.getClientId())) {
                            currentClientScope = scopeMapping;
                            break;
                        }
                    }
                    if (currentClientScope == null) {
                        currentClientScope = new ScopeMappingRepresentation();
                        currentClientScope.setClient(client.getClientId());
                        currentAppScopes.add(currentClientScope);
                    }
                    currentClientScope.role(scope.getName());
                }
            }
        }

        if (clientScopeReps.size() > 0) {
            rep.setClientScopeMappings(clientScopeReps);
        }

        // Finally users if needed
        if (includeUsers) {
            List<UserModel> allUsers = session.users().getUsers(realm, true);
            List<UserRepresentation> users = new ArrayList<UserRepresentation>();
            for (UserModel user : allUsers) {
                UserRepresentation userRep = exportUser(session, realm, user);
                users.add(userRep);
            }

            if (users.size() > 0) {
                rep.setUsers(users);
            }
        }

        return rep;
    }

    /**
     * Full export of application including claims and secret
     * @param client
     * @return full ApplicationRepresentation
     */
    public static ClientRepresentation exportClient(ClientModel client) {
        ClientRepresentation clientRep = ModelToRepresentation.toRepresentation(client);
        clientRep.setSecret(client.getSecret());
        return clientRep;
    }

    public static List<RoleRepresentation> exportRoles(Collection<RoleModel> roles) {
        List<RoleRepresentation> roleReps = new ArrayList<RoleRepresentation>();

        for (RoleModel role : roles) {
            RoleRepresentation roleRep = exportRole(role);
            roleReps.add(roleRep);
        }
        return roleReps;
    }

    public static List<String> getRoleNames(Collection<RoleModel> roles) {
        List<String> roleNames = new ArrayList<String>();
        for (RoleModel role : roles) {
            roleNames.add(role.getName());
        }
        return roleNames;
    }

    /**
     * Full export of role including composite roles
     * @param role
     * @return RoleRepresentation with all stuff filled (including composite roles)
     */
    public static RoleRepresentation exportRole(RoleModel role) {
        RoleRepresentation roleRep = ModelToRepresentation.toRepresentation(role);

        Set<RoleModel> composites = role.getComposites();
        if (composites != null && composites.size() > 0) {
            Set<String> compositeRealmRoles = null;
            Map<String, List<String>> compositeClientRoles = null;

            for (RoleModel composite : composites) {
                RoleContainerModel crContainer = composite.getContainer();
                if (crContainer instanceof RealmModel) {

                    if (compositeRealmRoles == null) {
                        compositeRealmRoles = new HashSet<>();
                    }
                    compositeRealmRoles.add(composite.getName());
                } else {
                    if (compositeClientRoles == null) {
                        compositeClientRoles = new HashMap<>();
                    }

                    ClientModel app = (ClientModel)crContainer;
                    String appName = app.getClientId();
                    List<String> currentAppComposites = compositeClientRoles.get(appName);
                    if (currentAppComposites == null) {
                        currentAppComposites = new ArrayList<>();
                        compositeClientRoles.put(appName, currentAppComposites);
                    }
                    currentAppComposites.add(composite.getName());
                }
            }

            RoleRepresentation.Composites compRep = new RoleRepresentation.Composites();
            if (compositeRealmRoles != null) {
                compRep.setRealm(compositeRealmRoles);
            }
            if (compositeClientRoles != null) {
                compRep.setClient(compositeClientRoles);
            }

            roleRep.setComposites(compRep);
        }

        return roleRep;
    }

    /**
     * Full export of user (including role mappings and credentials)
     *
     * @param user
     * @return fully exported user representation
     */
    public static UserRepresentation exportUser(KeycloakSession session, RealmModel realm, UserModel user) {
        UserRepresentation userRep = ModelToRepresentation.toRepresentation(user);

        // Social links
        Set<FederatedIdentityModel> socialLinks = session.users().getFederatedIdentities(user, realm);
        List<FederatedIdentityRepresentation> socialLinkReps = new ArrayList<FederatedIdentityRepresentation>();
        for (FederatedIdentityModel socialLink : socialLinks) {
            FederatedIdentityRepresentation socialLinkRep = exportSocialLink(socialLink);
            socialLinkReps.add(socialLinkRep);
        }
        if (socialLinkReps.size() > 0) {
            userRep.setFederatedIdentities(socialLinkReps);
        }

        // Role mappings
        Set<RoleModel> roles = user.getRoleMappings();
        List<String> realmRoleNames = new ArrayList<>();
        Map<String, List<String>> clientRoleNames = new HashMap<>();
        for (RoleModel role : roles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoleNames.add(role.getName());
            } else {
                ClientModel client = (ClientModel)role.getContainer();
                String clientId = client.getClientId();
                List<String> currentClientRoles = clientRoleNames.get(clientId);
                if (currentClientRoles == null) {
                    currentClientRoles = new ArrayList<>();
                    clientRoleNames.put(clientId, currentClientRoles);
                }

                currentClientRoles.add(role.getName());
            }
        }

        if (realmRoleNames.size() > 0) {
            userRep.setRealmRoles(realmRoleNames);
        }
        if (clientRoleNames.size() > 0) {
            userRep.setClientRoles(clientRoleNames);
        }

        // Credentials
        List<UserCredentialValueModel> creds = user.getCredentialsDirectly();
        List<CredentialRepresentation> credReps = new ArrayList<CredentialRepresentation>();
        for (UserCredentialValueModel cred : creds) {
            CredentialRepresentation credRep = exportCredential(cred);
            credReps.add(credRep);
        }
        userRep.setCredentials(credReps);
        userRep.setFederationLink(user.getFederationLink());

        // Grants
        List<UserConsentModel> consents = user.getConsents();
        LinkedList<UserConsentRepresentation> consentReps = new LinkedList<UserConsentRepresentation>();
        for (UserConsentModel consent : consents) {
            UserConsentRepresentation consentRep = ModelToRepresentation.toRepresentation(consent);
            consentReps.add(consentRep);
        }
        if (consentReps.size() > 0) {
            userRep.setClientConsents(consentReps);
        }

        // Service account
        if (user.getServiceAccountClientLink() != null) {
            String clientInternalId = user.getServiceAccountClientLink();
            ClientModel client = realm.getClientById(clientInternalId);
            if (client != null) {
                userRep.setServiceAccountClientId(client.getClientId());
            }
        }

        List<String> groups = new LinkedList<>();
        for (GroupModel group : user.getGroups()) {
            groups.add(ModelToRepresentation.buildGroupPath(group));
        }
        userRep.setGroups(groups);

        return userRep;
    }

    public static FederatedIdentityRepresentation exportSocialLink(FederatedIdentityModel socialLink) {
        FederatedIdentityRepresentation socialLinkRep = new FederatedIdentityRepresentation();
        socialLinkRep.setIdentityProvider(socialLink.getIdentityProvider());
        socialLinkRep.setUserId(socialLink.getUserId());
        socialLinkRep.setUserName(socialLink.getUserName());
        return socialLinkRep;
    }

    public static CredentialRepresentation exportCredential(UserCredentialValueModel userCred) {
        CredentialRepresentation credRep = new CredentialRepresentation();
        credRep.setType(userCred.getType());
        credRep.setDevice(userCred.getDevice());
        credRep.setHashedSaltedValue(userCred.getValue());
        if (userCred.getSalt() != null) credRep.setSalt(Base64.encodeBytes(userCred.getSalt()));
        credRep.setHashIterations(userCred.getHashIterations());
        credRep.setCounter(userCred.getCounter());
        credRep.setAlgorithm(userCred.getAlgorithm());
        credRep.setDigits(userCred.getDigits());
        return credRep;
    }

    // Streaming API

    public static void exportUsersToStream(KeycloakSession session, RealmModel realm, List<UserModel> usersToExport, ObjectMapper mapper, OutputStream os) throws IOException {
        JsonFactory factory = mapper.getJsonFactory();
        JsonGenerator generator = factory.createJsonGenerator(os, JsonEncoding.UTF8);
        try {
            if (mapper.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
                generator.useDefaultPrettyPrinter();
            }
            generator.writeStartObject();
            generator.writeStringField("realm", realm.getName());
            // generator.writeStringField("strategy", strategy.toString());
            generator.writeFieldName("users");
            generator.writeStartArray();

            for (UserModel user : usersToExport) {
                UserRepresentation userRep = ExportUtils.exportUser(session, realm, user);
                generator.writeObject(userRep);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }
}
