package org.keycloak.representations.admin.v2;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class OIDCClientRepresentation extends BaseClientRepresentation {

    public enum Flow {
        STANDARD,
        IMPLICIT,
        DIRECT_GRANT,
        SERVICE_ACCOUNT,
        TOKEN_EXCHANGE,
        DEVICE,
        CIBA
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Login flows that are enabled for this client")
    private Set<@NotBlank Flow> loginFlows = new LinkedHashSet<>();

    @Valid
    @JsonPropertyDescription("Authentication configuration for this client")
    private Auth auth;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Web origins that are allowed to make requests to this client")
    private Set<@NotBlank String> webOrigins = new LinkedHashSet<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Roles assigned to the service account")
    private Set<@NotBlank String> serviceAccountRoles = new LinkedHashSet<>();

    public OIDCClientRepresentation() {}

    public OIDCClientRepresentation(String clientId) {
        this.clientId = clientId;
    }

    public Set<Flow> getLoginFlows() {
        return loginFlows;
    }

    public void setLoginFlows(Set<Flow> loginFlows) {
        this.loginFlows = loginFlows;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getServiceAccountRoles() {
        return serviceAccountRoles;
    }

    public void setServiceAccountRoles(Set<String> serviceAccountRoles) {
        this.serviceAccountRoles = serviceAccountRoles;
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static class Auth {

        @JsonPropertyDescription("Which authentication method is used for this client")
        private String method;

        @JsonPropertyDescription("Secret used to authenticate this client with Secret authentication")
        private String secret;

        @JsonPropertyDescription("Public key used to authenticate this client with Signed JWT authentication")
        private String certificate;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Auth auth)) return false;
            return Objects.equals(enabled, auth.enabled)
                    && Objects.equals(method, auth.method)
                    && Objects.equals(secret, auth.secret)
                    && Objects.equals(certificate, auth.certificate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, method, secret, certificate);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static class ServiceAccount {

        @JsonPropertyDescription("Whether the service account is enabled")
        private Boolean enabled;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonPropertyDescription("Roles assigned to the service account")
        private Set<String> roles = new LinkedHashSet<String>();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServiceAccount that)) return false;
            return Objects.equals(enabled, that.enabled)
                    && Objects.equals(roles, that.roles);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, roles);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientRepresentation that = (ClientRepresentation) o;
        return Objects.equals(clientId, that.clientId)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(enabled, that.enabled)
                && Objects.equals(appUrl, that.appUrl)
                && Objects.equals(appRedirectUrls, that.appRedirectUrls)
                && Objects.equals(loginFlows, that.loginFlows)
                && Objects.equals(auth, that.auth)
                && Objects.equals(webOrigins, that.webOrigins)
                && Objects.equals(roles, that.roles)
                && Objects.equals(serviceAccount, that.serviceAccount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, displayName, description, protocol, enabled, appUrl, appRedirectUrls,
                loginFlows, auth, webOrigins, roles, serviceAccount
        );
    }
}
