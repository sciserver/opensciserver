package org.sciserver.springapp.racm.config;

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
public class OpenApiDefinition {
}
