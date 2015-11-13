package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum EventType {

    LOGIN(true),
    LOGIN_ERROR(true),
    REGISTER(true),
    REGISTER_ERROR(true),
    LOGOUT(true),
    LOGOUT_ERROR(true),

    CODE_TO_TOKEN(true),
    CODE_TO_TOKEN_ERROR(true),

    CLIENT_LOGIN(true),
    CLIENT_LOGIN_ERROR(true),

    REFRESH_TOKEN(false),
    REFRESH_TOKEN_ERROR(false),
    VALIDATE_ACCESS_TOKEN(false),
    VALIDATE_ACCESS_TOKEN_ERROR(false),

    FEDERATED_IDENTITY_LINK(true),
    FEDERATED_IDENTITY_LINK_ERROR(true),
    REMOVE_FEDERATED_IDENTITY(true),
    REMOVE_FEDERATED_IDENTITY_ERROR(true),

    UPDATE_EMAIL(true),
    UPDATE_EMAIL_ERROR(true),
    UPDATE_PROFILE(true),
    UPDATE_PROFILE_ERROR(true),
    UPDATE_PASSWORD(true),
    UPDATE_PASSWORD_ERROR(true),
    UPDATE_TOTP(true),
    UPDATE_TOTP_ERROR(true),
    VERIFY_EMAIL(true),
    VERIFY_EMAIL_ERROR(true),

    REMOVE_TOTP(true),
    REMOVE_TOTP_ERROR(true),

    REVOKE_GRANT(true),

    SEND_VERIFY_EMAIL(true),
    SEND_VERIFY_EMAIL_ERROR(true),
    SEND_RESET_PASSWORD(true),
    SEND_RESET_PASSWORD_ERROR(true),
    SEND_IDENTITY_PROVIDER_LINK(true),
    SEND_IDENTITY_PROVIDER_LINK_ERROR(true),
    RESET_PASSWORD(true),
    RESET_PASSWORD_ERROR(true),

    INVALID_SIGNATURE_ERROR(false),
    REGISTER_NODE(false),
    UNREGISTER_NODE(false),

    USER_INFO_REQUEST(false),
    USER_INFO_REQUEST_ERROR(false),

    IDENTITY_PROVIDER_LOGIN(false),
    IDENTITY_PROVIDER_LOGIN_ERROR(false),
    IDENTITY_PROVIDER_FIRST_LOGIN(true),
    IDENTITY_PROVIDER_FIRST_LOGIN_ERROR(true),
    IDENTITY_PROVIDER_RESPONSE(false),
    IDENTITY_PROVIDER_RESPONSE_ERROR(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR(false),
    IMPERSONATE(true),
    CUSTOM_REQUIRED_ACTION(true),
    CUSTOM_REQUIRED_ACTION_ERROR(true),
    EXECUTE_ACTIONS(true),
    EXECUTE_ACTIONS_ERROR(true),

    CLIENT_INFO(false),
    CLIENT_INFO_ERROR(false),
    CLIENT_REGISTER(true),
    CLIENT_REGISTER_ERROR(true),
    CLIENT_UPDATE(true),
    CLIENT_UPDATE_ERROR(true),
    CLIENT_DELETE(true),
    CLIENT_DELETE_ERROR(true);

    private boolean saveByDefault;

    EventType(boolean saveByDefault) {
        this.saveByDefault = saveByDefault;
    }

    public boolean isSaveByDefault() {
        return saveByDefault;
    }

}
