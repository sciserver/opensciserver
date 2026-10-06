package org.sciserver.springapp.racm.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

/**
 * Checks the hand-written @Parameter annotations against the bindings they describe.
 *
 * <p>Nothing validates them at runtime: a @Parameter naming an argument that does not exist, or
 * placing a header in the query string, renders as a plausible-looking entry in Swagger UI that
 * sends callers to the wrong place. Header names make this easy to get wrong, because they are
 * written as constants — and two classes in this tree define a SERVICE_TOKEN_HEADER with
 * different values.
 */
class DocumentedParameterTests {
    private static final String BASE_PACKAGE = "org.sciserver.springapp.racm";

    private static List<Class<?>> restControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> found = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                found.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return found;
    }

    /** Wire name to location, for every argument of the method Spring binds from the request. */
    private static Map<String, ParameterIn> boundArguments(Method method) {
        Map<String, ParameterIn> bound = new HashMap<>();
        for (Parameter parameter : method.getParameters()) {
            RequestParam query = parameter.getAnnotation(RequestParam.class);
            PathVariable path = parameter.getAnnotation(PathVariable.class);
            RequestHeader header = parameter.getAnnotation(RequestHeader.class);

            if (query != null) {
                bound.put(wireName(query.name(), query.value(), parameter), ParameterIn.QUERY);
            } else if (path != null) {
                bound.put(wireName(path.name(), path.value(), parameter), ParameterIn.PATH);
            } else if (header != null) {
                bound.put(wireName(header.name(), header.value(), parameter), ParameterIn.HEADER);
            }
        }
        return bound;
    }

    private static String wireName(String name, String value, Parameter parameter) {
        if (!name.isEmpty()) {
            return name;
        }
        return value.isEmpty() ? parameter.getName() : value;
    }

    @Test
    void everyDocumentedParameterMatchesTheArgumentItDescribes() {
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Operation operation = method.getAnnotation(Operation.class);
                if (operation == null) {
                    continue;
                }
                Map<String, ParameterIn> bound = boundArguments(method);
                for (io.swagger.v3.oas.annotations.Parameter documented : operation.parameters()) {
                    checked++;
                    ParameterIn actual = bound.get(documented.name());
                    if (actual == null) {
                        problems.add(String.format("%s.%s documents '%s', which it does not bind; it binds %s",
                                controller.getSimpleName(), method.getName(), documented.name(), bound.keySet()));
                    } else if (actual != documented.in()) {
                        problems.add(String.format("%s.%s documents '%s' as %s, but binds it from the %s",
                                controller.getSimpleName(), method.getName(), documented.name(),
                                documented.in(), actual));
                    }
                }
            }
        }

        assertTrue(checked > 0, "no documented parameters were found to check");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyBoundArgumentOfADocumentedOperationIsDescribed() {
        List<String> problems = new ArrayList<>();

        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Operation operation = method.getAnnotation(Operation.class);
                if (operation == null) {
                    continue;
                }
                List<String> documented = new ArrayList<>();
                for (io.swagger.v3.oas.annotations.Parameter parameter : operation.parameters()) {
                    documented.add(parameter.name());
                }
                for (String name : boundArguments(method).keySet()) {
                    if (!documented.contains(name)) {
                        problems.add(String.format("%s.%s binds '%s' but does not describe it",
                                controller.getSimpleName(), method.getName(), name));
                    }
                }
            }
        }

        assertFalse(restControllers().isEmpty(), "no REST controllers were found to check");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
