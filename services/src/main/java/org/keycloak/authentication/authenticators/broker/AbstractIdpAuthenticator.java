package org.keycloak.authentication.authenticators.broker;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractIdpAuthenticator implements Authenticator {

    // The clientSession note encapsulating all the BrokeredIdentityContext info. When this note is in clientSession, we know that firstBrokerLogin flow is in progress
    public static final String BROKERED_CONTEXT_NOTE = "BROKERED_CONTEXT";

    // The clientSession note with all the info about existing user
    public static final String EXISTING_USER_INFO = "EXISTING_USER_INFO";

    // The clientSession note flag to indicate that email provided by identityProvider was changed on updateProfile page
    public static final String UPDATE_PROFILE_EMAIL_CHANGED = "UPDATE_PROFILE_EMAIL_CHANGED";

    // The clientSession note flag to indicate if re-authentication after first broker login happened in different browser window. This can happen for example during email verification
    public static final String IS_DIFFERENT_BROWSER = "IS_DIFFERENT_BROWSER";

    // The clientSession note flag to indicate that updateProfile page will be always displayed even if "updateProfileOnFirstLogin" is off
    public static final String ENFORCE_UPDATE_PROFILE = "ENFORCE_UPDATE_PROFILE";

    // clientSession.note flag specifies if we imported new user to keycloak (true) or we just linked to an existing keycloak user (false)
    public static final String BROKER_REGISTERED_NEW_USER = "BROKER_REGISTERED_NEW_USER";


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ClientSessionModel clientSession = context.getClientSession();

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSession, BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), clientSession);

        if (!brokerContext.getIdpConfig().isEnabled()) {
            sendFailureChallenge(context, Errors.IDENTITY_PROVIDER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        authenticateImpl(context, serializedCtx, brokerContext);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        ClientSessionModel clientSession = context.getClientSession();

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSession, BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), clientSession);

        if (!brokerContext.getIdpConfig().isEnabled()) {
            sendFailureChallenge(context, Errors.IDENTITY_PROVIDER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR, AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        actionImpl(context, serializedCtx, brokerContext);
    }

    protected abstract void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext);
    protected abstract void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext);

    protected void sendFailureChallenge(AuthenticationFlowContext context, String eventError, String errorMessage, AuthenticationFlowError flowError) {
        context.getEvent().user(context.getUser())
                .error(eventError);
        Response challengeResponse = context.form()
                .setError(errorMessage)
                .createErrorPage();
        context.failureChallenge(flowError, challengeResponse);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }

    public static UserModel getExistingUser(KeycloakSession session, RealmModel realm, ClientSessionModel clientSession) {
        String existingUserId = clientSession.getNote(EXISTING_USER_INFO);
        if (existingUserId == null) {
            throw new AuthenticationFlowException("Unexpected state. There is no existing duplicated user identified in ClientSession",
                    AuthenticationFlowError.INTERNAL_ERROR);
        }

        ExistingUserInfo duplication = ExistingUserInfo.deserialize(existingUserId);

        UserModel existingUser = session.users().getUserById(duplication.getExistingUserId(), realm);
        if (existingUser == null) {
            throw new AuthenticationFlowException("User with ID '" + existingUserId + "' not found.", AuthenticationFlowError.INVALID_USER);
        }

        if (!existingUser.isEnabled()) {
            throw new AuthenticationFlowException("User with ID '" + existingUserId + "', username '" + existingUser.getUsername() + "' disabled.", AuthenticationFlowError.USER_DISABLED);
        }

        return existingUser;
    }
}
