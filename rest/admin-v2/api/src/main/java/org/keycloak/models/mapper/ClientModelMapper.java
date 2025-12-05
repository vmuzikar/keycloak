package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.ServiceException;

import org.mapstruct.Context;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ClientModelMapper {

    BaseClientRepresentation fromModel(@Context KeycloakSession session, ClientModel model);

    ClientModel toModel(@Context KeycloakSession session, @Context RealmModel realm, @MappingTarget ClientModel existingModel, BaseClientRepresentation rep) throws ServiceException;

    ClientModel toModel(@Context KeycloakSession session, @Context RealmModel realm, BaseClientRepresentation rep) throws ServiceException;

    @Mapping(target = "displayName", source = "name")
    @Mapping(target = "appUrl", source = "baseUrl")
    @Mapping(target = "roles", source = "rolesStream", qualifiedByName = "getRoleStrings")
    @interface ModelToBaseRep {}

    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "baseUrl", source = "appUrl")
    @interface BaseRepToModel {}

    @ObjectFactory
    default ClientModel createClientModel(@Context RealmModel realm, BaseClientRepresentation rep) {
        // dummy add/remove to obtain a detached model
        var model = realm.addClient(rep.getClientId());
        realm.removeClient(model.getId());
        return model;
    }

    @Named("getRoleStrings")
    default Set<String> getRoleStrings(Stream<RoleModel> stream) {
        return stream.map(RoleModel::getName).collect(Collectors.toSet());
    }
}
