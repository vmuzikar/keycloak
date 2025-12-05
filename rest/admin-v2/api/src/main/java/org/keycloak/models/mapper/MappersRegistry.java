package org.keycloak.models.mapper;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.mapstruct.factory.Mappers;

import java.util.Map;

public class MappersRegistry {
    private static final Map<Class<? extends BaseClientRepresentation>, Class<? extends ClientModelMapper>> REP_TO_MAPPER_CLASS = Map.of(
        OIDCClientRepresentation.class, OIDCClientModelMapper.class
    );

    private static final Map<String, Class<? extends ClientModelMapper>> PROTOCOL_TO_REP_CLASS = Map.of(
        "openid-connect", OIDCClientModelMapper.class
    );

    public <T extends BaseClientRepresentation> ClientModelMapper clients(Class<T> repClass) {
        return Mappers.getMapper(REP_TO_MAPPER_CLASS.get(repClass));
    }

    public ClientModelMapper clients(String modelProtocolName) {
        return Mappers.getMapper(PROTOCOL_TO_REP_CLASS.get(modelProtocolName));
    }
}
