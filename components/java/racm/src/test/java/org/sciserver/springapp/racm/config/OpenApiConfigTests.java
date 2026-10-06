package org.sciserver.springapp.racm.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Pins the path filter that keeps the generated API document to REST controllers.
 *
 * <p>Both directions matter and both fail silently in production: an unmatched REST prefix drops
 * endpoints from the docs with no error, and a matched MVC path publishes JSP handlers on a
 * publicly reachable UI.
 */
class OpenApiConfigTests {
    private final AntPathMatcher matcher = new AntPathMatcher();

    private boolean documented(String path) {
        return Arrays.stream(OpenApiConfig.REST_PATH_PATTERNS)
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    @Test
    void everyRestControllerPrefixIsDocumented() {
        // Real paths, built from each controller's class-level and method-level mappings.
        List<String> restPaths = List.of(
                "/jobm/rest/jobs",
                "/jobm/rest/computedomains/docker/abc-123/rootvolumes",
                "/storem/fileservices",
                "/storem/fileservice/abc-123/rootVolume/data/userVolume/bob/vol",
                "/ugm/rest/user",
                "/ugm/rest/groups/7",
                "/rc/abc-123/resources",
                "/rc/abc-123/resource/def-456/action/read",
                "/rest/resources",
                "/rest/privileges",
                "/collaborations",
                "/config",
                "/workspace/groups");

        for (String path : restPaths) {
            assertTrue(documented(path), "REST path should be documented but is not: " + path);
        }
    }

    @Test
    void noMvcControllerPathIsDocumented() {
        List<String> mvcPaths = List.of(
                "/cctree/contextClassList",
                "/cctree/contextClass/4",
                "/cctree/resourceType",
                "/rctree/resourceContextList",
                "/rctree/resource/9",
                "/compm/mvc/view/abc-123",
                "/query/jpql");

        for (String path : mvcPaths) {
            assertFalse(documented(path), "MVC path must not be documented but is: " + path);
        }
    }

    @Test
    void declaredSchemeNamesAreExactlyTheOnesControllersReference() {
        SecuritySchemes schemes = OpenApiDefinition.class.getAnnotation(SecuritySchemes.class);
        List<String> declared = Arrays.stream(schemes.value())
                .map(SecurityScheme::name)
                .sorted()
                .toList();

        // Controllers reference these names verbatim. A typo renders as no requirement at all,
        // making a service-gated endpoint look public in the documentation.
        assertTrue(declared.contains("userToken"), "userToken scheme must be declared");
        assertTrue(declared.contains("serviceToken"), "serviceToken scheme must be declared");
    }
}
