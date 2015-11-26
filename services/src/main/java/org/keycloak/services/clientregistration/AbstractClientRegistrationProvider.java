package org.keycloak.services.clientregistration;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationProvider implements ClientRegistrationProvider {

    protected KeycloakSession session;
    protected EventBuilder event;
    protected ClientRegistrationAuth auth;

    public AbstractClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    public ClientRepresentation create(ClientRepresentation client) {
        event.event(EventType.CLIENT_REGISTER);

        auth.requireCreate();

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, session.getContext().getRealm(), client, true);
            if (client.getClientId() == null) {
                clientModel.setClientId(clientModel.getId());
            }

            client = ModelToRepresentation.toRepresentation(clientModel);

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel);

            client.setRegistrationAccessToken(registrationAccessToken);

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                initialAccessModel.decreaseRemainingCount();
            }

            event.client(client.getClientId()).success();
            return client;
        } catch (ModelDuplicateException e) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use", Response.Status.BAD_REQUEST);
        }
    }

    public ClientRepresentation get(String clientId) {
        event.event(EventType.CLIENT_INFO);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public ClientRepresentation update(String clientId, ClientRepresentation rep) {
        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        if (!client.getClientId().equals(rep.getClientId())) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier modified", Response.Status.BAD_REQUEST);
        }

        RepresentationToModel.updateClient(rep, client);
        rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public void delete(String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        if (session.getContext().getRealm().removeClient(client.getId())) {
            event.client(client.getClientId()).success();
        } else {
            throw new ForbiddenException();
        }
    }

    @Override
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

}
