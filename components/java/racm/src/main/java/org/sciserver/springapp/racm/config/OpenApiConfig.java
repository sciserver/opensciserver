package org.sciserver.springapp.racm.config;

import java.util.List;

import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springdoc.core.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.sciserver.springapp.racm.jobm.application.COMPMRequiredInjector;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;

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

    static {
        // Neither of these is a request parameter, but springdoc cannot tell: it sees an
        // unannotated handler argument and documents it as one the caller must supply.
        //
        //   UserProfile  is resolved from the X-Auth-Token header by
        //                SciServerHeaderAuthenticationFilter and injected via
        //                @AuthenticationPrincipal.
        //   COMPMInfo    is injected by COMPMRequiredInjector, a @ControllerAdvice that
        //                resolves it from the X-Service-Auth-ID header.
        //
        // Registering them here removes a bogus "up" query parameter from 87 operations and a
        // bogus "compm" one from 8, without annotating every handler individually. The tokens
        // themselves stay documented, as the userToken and serviceToken security schemes.
        SpringDocUtils.getConfig()
                .addRequestWrapperToIgnore(UserProfile.class)
                .addRequestWrapperToIgnore(COMPMRequiredInjector.COMPMInfo.class);
    }

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
    /**
     * Restricts Swagger UI's "Try it out" button to GET requests.
     *
     * <p>Half of RACM's documented surface mutates state: 49 of 98 operations, including nine
     * DELETEs that unregister file services and drop root volumes, user volumes and groups, and
     * POST /computedomains/docker, which replaces a whole domain and deletes any image, volume
     * container or root volume absent from the request.
     *
     * <p>Authentication bounds who can do that damage, not how easily. Reading stays fully
     * interactive, which is where most of the UI's value is; anything that writes has to be issued
     * deliberately from curl or a client.
     *
     * <p>Set here rather than as a springdoc.swagger-ui.* property on purpose: the deployment sets
     * SPRING_CONFIG_LOCATION=/etc/secrets/, which replaces Spring's default config locations, so a
     * property in the packaged application.properties is not read in production -- Try it out would
     * stay fully enabled on the public UI while appearing restricted in development.
     *
     * <p>Done as a BeanPostProcessor, and declared static so the processor is available before this
     * configuration class is instantiated. SwaggerUiConfigParameters takes SwaggerUiConfigProperties
     * in its constructor and snapshots it, so anything that mutates the properties later -- an
     * InitializingBean callback, for instance -- is simply missed, and the served swagger-config
     * carries no supportedSubmitMethods at all.
     *
     * @return a post-processor limiting the Swagger UI submit methods to GET
     */
    @Bean
    static BeanPostProcessor restrictSwaggerUiToReadOnly() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof SwaggerUiConfigProperties properties) {
                    properties.setSupportedSubmitMethods(List.of("get"));
                }
                return bean;
            }
        };
    }

    @Bean
    GroupedOpenApi racmRestApi() {
        return GroupedOpenApi.builder()
                .group("racm")
                .pathsToMatch(REST_PATH_PATTERNS)
                .build();
    }
}
