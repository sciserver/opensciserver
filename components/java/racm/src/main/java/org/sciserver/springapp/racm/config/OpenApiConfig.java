package org.sciserver.springapp.racm.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * OpenAPI document metadata and authentication schemes for the RACM REST API.
 *
 * <p>These annotations are documentation only. Authentication is enforced by Spring Security and
 * the request filters (SciServerHeaderAuthenticationFilter, FileServiceAuthenticationInjector,
 * COMPMRequired). If an annotation and the real gate ever disagree, the annotation is wrong and
 * the endpoint is still protected.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "SciServer RACM API",
        version = "v1",
        description = "Resource Access Control Management. Manages users, groups, resources and "
                + "the rights to act on them, and brokers access to compute domains and file "
                + "services. Most endpoints require a user token; those marked with the "
                + "serviceToken requirement are called by other SciServer components rather than "
                + "by end users."),
    security = @SecurityRequirement(name = "userToken"))
@SecuritySchemes({
    @SecurityScheme(
        name = "userToken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Auth-Token",
        description = "SciServer user token, obtained from the login portal."),
    @SecurityScheme(
        name = "serviceToken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Service-Auth-ID",
        description = "Service token identifying a registered SciServer component, such as a "
                + "FileService instance or a COMPM. Not a user credential.")
})
public class OpenApiConfig {

    /**
     * Path patterns for RACM's REST controllers.
     *
     * <p>springdoc documents every handler Spring registers, which includes the MVC controllers
     * that serve the admin JSP pages (/cctree, /rctree, /compm/mvc, /query). Those are disjoint
     * from every prefix below, so matching these patterns excludes them by construction.
     *
     * <p>A new controller on a prefix not listed here is silently absent from the published
     * documentation. OpenApiConfigTests guards both directions.
     */
    static final String[] REST_PATH_PATTERNS = {
        "/jobm/rest/**",
        "/storem/**",
        "/ugm/rest/**",
        "/rc/**",
        "/rest/**",
        "/collaborations/**",
        "/config/**",
        "/workspace/**",
    };

    /**
     * Restricts the generated document to the REST API.
     *
     * <p>Expressed in code rather than as a springdoc.paths-to-match property because the
     * deployment sets SPRING_CONFIG_LOCATION=/etc/secrets/, which replaces Spring's default
     * config locations. The packaged application.properties is therefore not read in production,
     * so a property set there would work locally and silently vanish in the deployed service --
     * where the UI is public.
     */
    @Bean
    GroupedOpenApi racmRestApi() {
        return GroupedOpenApi.builder()
                .group("racm")
                .pathsToMatch(REST_PATH_PATTERNS)
                .build();
    }
}
