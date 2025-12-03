package org.keycloak.representations.admin.v2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.URL;

import org.keycloak.representations.admin.v2.validation.CreateClient;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.SIMPLE_NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "protocol"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OIDCClientRepresentation.class, name = "openid-connect"),
    @JsonSubTypes.Type(value = SAMLClientRepresentation.class, name = "saml")
})
public abstract class BaseClientRepresentation {
    @NotBlank(groups = CreateClient.class)
    @JsonPropertyDescription("ID uniquely identifying this client")
    protected String clientId;

    @JsonPropertyDescription("Human readable name of the client")
    private String displayName;

    @JsonPropertyDescription("Human readable description of the client")
    private String description;

    @JsonPropertyDescription("Whether this client is enabled")
    private Boolean enabled;

    @URL
    @JsonPropertyDescription("URL to the application's homepage that is represented by this client")
    private String appUrl;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("URIs that the browser can redirect to after login")
    private Set<@NotBlank @URL(message = "Each redirect URL must be valid") String> redirectUris = new LinkedHashSet<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Roles associated with this client")
    private Set<@NotBlank String> roles = new LinkedHashSet<>();

    @JsonIgnore
    protected Map<String, Object> additionalFields = new LinkedHashMap<String, Object>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    @JsonAnySetter
    public void setAdditionalField(String name, Object value) {
        this.additionalFields.put(name, value);
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields;
    }
}
