package org.keycloak.quarkus.runtime.oas;

import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.jboss.jandex.IndexView;

@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
public class OASModelFilter implements OASFilter {

    private final IndexView indexView;

    public OASModelFilter(IndexView indexView) {
        this.indexView = indexView;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Map<String, PathItem> newPaths = openAPI.getPaths().getPathItems().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> sortOperationsByMethod(entry.getValue())
                ));

        // Replace ALL Paths with sorted Paths
        var paths = OASFactory.createPaths();
        newPaths.forEach(paths::addPathItem);
        openAPI.setPaths(paths);
    }

    private PathItem sortOperationsByMethod(PathItem pathItem) {
        PathItem sortedPathItem = OASFactory.createPathItem();

        // Add operations order: GET -> POST -> PUT -> PATCH -> DELETE -> HEAD -> OPTIONS -> TRACE
        if (pathItem.getGET() != null) {
            sortedPathItem.setGET(pathItem.getGET());
        }
        if (pathItem.getPOST() != null) {
            sortedPathItem.setPOST(pathItem.getPOST());
        }
        if (pathItem.getPUT() != null) {
            sortedPathItem.setPUT(pathItem.getPUT());
        }
        if (pathItem.getPATCH() != null) {
            sortedPathItem.setPATCH(pathItem.getPATCH());
        }
        if (pathItem.getDELETE() != null) {
            sortedPathItem.setDELETE(pathItem.getDELETE());
        }
        if (pathItem.getHEAD() != null) {
            sortedPathItem.setHEAD(pathItem.getHEAD());
        }
        if (pathItem.getOPTIONS() != null) {
            sortedPathItem.setOPTIONS(pathItem.getOPTIONS());
        }
        if (pathItem.getTRACE() != null) {
            sortedPathItem.setTRACE(pathItem.getTRACE());
        }

        sortedPathItem.setSummary(pathItem.getSummary());
        sortedPathItem.setDescription(pathItem.getDescription());
        sortedPathItem.setServers(pathItem.getServers());
        sortedPathItem.setParameters(pathItem.getParameters());

        return sortedPathItem;
    }
}
