package org.sciserver.springapp.racm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.core.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.storem.controller.FileServiceLinkedServiceController;
import org.sciserver.springapp.racm.storem.controller.FileServiceTokenRequiredController;
import org.sciserver.springapp.racm.storem.controller.FileServiceUserRequiredController;

/**
 * Generates the API document the way the running service does, and fails if springdoc cannot.
 *
 * <p>The annotations are only ever exercised by the generator. Nothing else notices when a pair of
 * them cannot be turned into a document: the first symptom is a 500 from /v3/api-docs, which takes
 * the whole reference page down rather than degrading one entry.
 */
@SpringBootTest(classes = ApiDocumentGenerationTests.Config.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ApiDocumentGenerationTests {

    @EnableWebMvc
    @ContextConfiguration
    @ImportAutoConfiguration({JacksonAutoConfiguration.class})
    @org.springframework.context.annotation.Import({
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        SpringDocConfigProperties.class,
        OpenApiConfig.class,
        OpenApiDefinition.class,
        FileServiceUserRequiredController.class,
        FileServiceTokenRequiredController.class,
        FileServiceLinkedServiceController.class,
    })
    static class Config {
        @MockBean
        private FileServiceManager fileServiceManager;
    }

    @Autowired
    private OpenApiWebMvcResource resource;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    void exactlyOneBeanCarriesTheDocumentDefinition() {
        // springdoc picks one arbitrarily when there are several, so a second one silently
        // decides the document's title, version and security schemes. A bean from an @Bean
        // method counts as carrying its declaring class's annotations, which is why the
        // definition lives on a class with no beans in it.
        assertEquals(List.of("org.sciserver.springapp.racm.config.OpenApiDefinition"),
                List.copyOf(context.getBeansWithAnnotation(OpenAPIDefinition.class).keySet()),
                "exactly one bean should carry @OpenAPIDefinition");
    }

    @Test
    void theStoremControllersProduceADocument() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        List<String> problems = new ArrayList<>();
        String document = null;
        try {
            document = new String(resource.openapiJson(request, "/v3/api-docs", Locale.ENGLISH),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            problems.add(e.getMessage());
        }

        assertEquals(List.of(), problems, "springdoc could not build the document");
        assertTrue(document != null && document.contains("fileservice"),
                "the document should describe the file service endpoints");
        // OpenAPI keys operations by path and method, so the two handlers sharing
        // GET /storem/fileservice/{id} can only ever appear as one entry.
        String sharedPath = "\"/storem/fileservice/{fileServiceIdentifer}\"";
        assertEquals(1, document.split(java.util.regex.Pattern.quote(sharedPath), -1).length - 1,
                "the shared path should appear exactly once");
    }
}
