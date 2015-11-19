package org.keycloak.models.sessions.infinispan.compat;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.compat.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureKey;
import org.keycloak.models.sessions.infinispan.compat.entities.ClientInitialAccessEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProviderFactory {

    private ConcurrentHashMap<String, UserSessionEntity> userSessions = new ConcurrentHashMap<String, UserSessionEntity>();

    private ConcurrentHashMap<String, ClientSessionEntity> clientSessions = new ConcurrentHashMap<String, ClientSessionEntity>();

    private ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures = new ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity>();
    private final ConcurrentHashMap<String, String> userSessionsByBrokerSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> userSessionsByBrokerUserId = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, UserSessionEntity> offlineUserSessions = new ConcurrentHashMap<String, UserSessionEntity>();
    private ConcurrentHashMap<String, ClientSessionEntity> offlineClientSessions = new ConcurrentHashMap<String, ClientSessionEntity>();

    private ConcurrentHashMap<String, ClientInitialAccessEntity> clientInitialAccess = new ConcurrentHashMap<>();

    public UserSessionProvider create(KeycloakSession session) {
        return new MemUserSessionProvider(session, userSessions, userSessionsByBrokerSessionId, userSessionsByBrokerUserId, clientSessions, loginFailures,
                offlineUserSessions, offlineClientSessions, clientInitialAccess);
    }

    public void close() {
        userSessions.clear();
        loginFailures.clear();
        userSessionsByBrokerSessionId.clear();
        userSessionsByBrokerUserId.clear();
    }

}
