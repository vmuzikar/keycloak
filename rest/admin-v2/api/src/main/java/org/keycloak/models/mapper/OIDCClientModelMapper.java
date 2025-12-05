package org.keycloak.models.mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.services.ServiceException;

import org.mapstruct.Context;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.SubclassExhaustiveStrategy;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface OIDCClientModelMapper extends ClientModelMapper {

    @ModelToBaseRep
    @Mapping(target = "loginFlows", source = ".", qualifiedByName = "createLoginFlows")
    @Mapping(target = "auth", source = ".", qualifiedByName = "createAuth")
    @Mapping(target = "auth.method", source = "clientAuthenticatorType")
    @Mapping(target = "auth.secret", source = "secret")
    // TODO: auth.certificate
    OIDCClientRepresentation fromModel(@Context KeycloakSession session, ClientModel model) throws ServiceException;

    @BaseRepToModel
    @Mapping(target = "publicClient", source = "auth", qualifiedByName = "isPublicClient")
    @Mapping(target = "clientAuthenticatorType", source = "auth.method")
    @Mapping(target = "secret", source = "auth.secret")
    @Mapping(target = "standardFlowEnabled", source = "loginFlows", qualifiedByName = "isStandardFlowEnabled")
    @Mapping(target = "implicitFlowEnabled", source = "loginFlows", qualifiedByName = "isImplicitFlowEnabled")
    @Mapping(target = "directAccessGrantsEnabled", source = "loginFlows", qualifiedByName = "isDirectGrantFlowEnabled")
    ClientModel toExistingModel(@Context KeycloakSession session, @Context RealmModel realm, @MappingTarget ClientModel existingModel, OIDCClientRepresentation rep) throws ServiceException;

    @InheritConfiguration(name = "toExistingModel")
    ClientModel toModel(@Context KeycloakSession session, @Context RealmModel realm, OIDCClientRepresentation rep) throws ServiceException;

    @Named("createLoginFlows")
    default Set<OIDCClientRepresentation.Flow> createLoginFlows(ClientModel model) {
        Set<OIDCClientRepresentation.Flow> flows = new HashSet<>();
        if (model.isStandardFlowEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.STANDARD);
        }
        if (model.isImplicitFlowEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.IMPLICIT);
        }
        if (model.isDirectAccessGrantsEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.DIRECT_GRANT);
        }
        // TODO: device flow
        if (model.isServiceAccountsEnabled()) {
            flows.add(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);
        }
        return flows;
    }

    @Named("isStandardFlowEnabled")
    default boolean isStandardFlowEnabled(Set<OIDCClientRepresentation.Flow> flows) {
        return flows.contains(OIDCClientRepresentation.Flow.STANDARD);
    }

    @Named("isImplicitFlowEnabled")
    default boolean isImplicitFlowEnabled(Set<OIDCClientRepresentation.Flow> flows) {
        return flows.contains(OIDCClientRepresentation.Flow.IMPLICIT);
    }

    @Named("isDirectGrantFlowEnabled")
    default boolean isDirectGrantFlowEnabled(Set<OIDCClientRepresentation.Flow> flows) {
        return flows.contains(OIDCClientRepresentation.Flow.DIRECT_GRANT);
    }

    @Named("createAuth")
    default OIDCClientRepresentation.Auth createAuth(ClientModel client) {
        if (client.isPublicClient()) {
            return new OIDCClientRepresentation.Auth();
        }
        return null;
    }

    @Named("isPublicClient")
    default boolean isPublicClient(OIDCClientRepresentation.Auth auth) {
        return auth != null;
    }

    @Named("getServiceAccountRoles")
    default Set<String> getServiceAccountRoles(@Context KeycloakSession session, ClientModel client) {
        if (client.isServiceAccountsEnabled()) {
            return session.users().getServiceAccount(client)
                    .getRoleMappingsStream()
                    .map(RoleModel::getName)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
