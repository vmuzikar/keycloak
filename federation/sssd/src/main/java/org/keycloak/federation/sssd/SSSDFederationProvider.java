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

package org.keycloak.federation.sssd;

import org.freedesktop.dbus.Variant;
import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.federation.sssd.api.Sssd;
import org.keycloak.federation.sssd.impl.PAMAuthenticator;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.UserManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPI provider implementation to retrieve data from SSSD and authenticate
 * against PAM
 *
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProvider implements UserFederationProvider {

    private static final Logger logger = Logger.getLogger(SSSDFederationProvider.class);

    protected static final Set<String> supportedCredentialTypes = new HashSet<>();
    private final SSSDFederationProviderFactory factory;
    protected KeycloakSession session;
    protected UserFederationProviderModel model;

    public SSSDFederationProvider(KeycloakSession session, UserFederationProviderModel model, SSSDFederationProviderFactory sssdFederationProviderFactory) {
        this.session = session;
        this.model = model;
        this.factory = sssdFederationProviderFactory;
    }

    static {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }


    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return findOrCreateAuthenticatedUser(realm, username);
    }

    /**
     * Called after successful authentication
     *
     * @param realm    realm
     * @param username username without realm prefix
     * @return user if found or successfully created. Null if user with same username already exists, but is not linked to this provider
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debug("SSSD authenticated user " + username + " found in Keycloak storage");

            if (!model.getId().equals(user.getFederationLink())) {
                logger.warn("User with username " + username + " already exists, but is not linked to provider [" + model.getDisplayName() + "]");
                return null;
            } else {
                UserModel proxied = validateAndProxy(realm, user);
                if (proxied != null) {
                    return proxied;
                } else {
                    logger.warn("User with username " + username + " already exists and is linked to provider [" + model.getDisplayName() +
                            "] but principal is not correct.");
                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userStorage());
                }
            }
        }

        logger.debug("SSSD authenticated user " + username + " not in Keycloak storage. Creating...");
        return importUserToKeycloak(realm, username);
    }

    protected UserModel importUserToKeycloak(RealmModel realm, String username) {
        Sssd sssd = new Sssd(username);
        Map<String, Variant> sssdUser = sssd.getUserAttributes();
        logger.debugf("Creating SSSD user: %s to local Keycloak storage", username);
        UserModel user = session.userStorage().addUser(realm, username);
        user.setEnabled(true);
        user.setEmail(Sssd.getRawAttribute(sssdUser.get("mail")));
        user.setFirstName(Sssd.getRawAttribute(sssdUser.get("givenname")));
        user.setLastName(Sssd.getRawAttribute(sssdUser.get("sn")));
        for (String s : sssd.getUserGroups()) {
            GroupModel group = KeycloakModelUtils.findGroupByPath(realm, "/" + s);
            if (group == null) {
                group = session.realms().createGroup(realm, s);
            }
            user.joinGroup(group);
        }
        user.setFederationLink(model.getId());
        return validateAndProxy(realm, user);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete  We don't care about the realm being removed
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete we dont'care if a role is removed

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        // complete we dont'care if a role is removed

    }

    @Override
    public boolean isValid(RealmModel realm, UserModel local) {
        Map<String, Variant> attributes = new Sssd(local.getUsername()).getUserAttributes();
        return Sssd.getRawAttribute(attributes.get("mail")).equalsIgnoreCase(local.getEmail());
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return supportedCredentialTypes;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel) || !CredentialModel.PASSWORD.equals(input.getType())) return false;
        throw new ModelReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        PAMAuthenticator pam = factory.createPAMAuthenticator(user.getUsername(), cred.getValue());
        return (pam.authenticate() != null);
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        return CredentialValidationOutput.failed();
    }

    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (isValid(realm, local)) {
            return new ReadonlySSSDUserModelDelegate(local, this);
        } else {
            return null;
        }
    }

    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Registration not supported");
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void close() {
        Sssd.disconnect();
    }
}
