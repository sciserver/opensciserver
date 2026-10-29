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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
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

    /** Full paths served by a method, from its class-level and method-level mappings. */
    private static List<String> pathsOf(Class<?> controller, Method method) {
        RequestMapping base = controller.getAnnotation(RequestMapping.class);
        List<String> prefixes = new ArrayList<>();
        if (base == null || base.value().length == 0) {
            prefixes.add("");
        } else {
            prefixes.addAll(List.of(base.value()));
        }

        String verb = null;
        String[] suffixes = null;
        if (method.getAnnotation(GetMapping.class) != null) {
            verb = "GET";
            suffixes = method.getAnnotation(GetMapping.class).value();
        } else if (method.getAnnotation(PostMapping.class) != null) {
            verb = "POST";
            suffixes = method.getAnnotation(PostMapping.class).value();
        } else if (method.getAnnotation(PutMapping.class) != null) {
            verb = "PUT";
            suffixes = method.getAnnotation(PutMapping.class).value();
        } else if (method.getAnnotation(DeleteMapping.class) != null) {
            verb = "DELETE";
            suffixes = method.getAnnotation(DeleteMapping.class).value();
        } else if (method.getAnnotation(PatchMapping.class) != null) {
            verb = "PATCH";
            suffixes = method.getAnnotation(PatchMapping.class).value();
        }

        List<String> paths = new ArrayList<>();
        if (verb == null) {
            return paths;
        }
        if (suffixes.length == 0) {
            suffixes = new String[] {""};
        }
        for (String prefix : prefixes) {
            for (String suffix : suffixes) {
                String full = ("/" + prefix + "/" + suffix).replaceAll("/+", "/");
                if (full.length() > 1 && full.endsWith("/")) {
                    full = full.substring(0, full.length() - 1);
                }
                paths.add(verb + " " + full);
            }
        }
        return paths;
    }

    @Test
    void noTwoDocumentedEndpointsShareAPathAndMethod() {
        // OpenAPI keys an operation by path and method, so two handlers that share both can only
        // ever be one entry. springdoc does not degrade that path, it fails the whole document
        // with a duplicate-key error, taking the reference page down. Spring itself allows the
        // pair, distinguishing them by a header condition it can express and OpenAPI cannot; the
        // way out is to hide one and describe its behaviour on the other.
        Map<String, List<String>> servedBy = new HashMap<>();
        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.getAnnotation(Hidden.class) != null
                        || controller.getAnnotation(Hidden.class) != null) {
                    continue;
                }
                for (String endpoint : pathsOf(controller, method)) {
                    servedBy.computeIfAbsent(endpoint, key -> new ArrayList<>())
                            .add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }

        List<String> clashes = new ArrayList<>();
        servedBy.forEach((endpoint, handlers) -> {
            if (handlers.size() > 1) {
                clashes.add(endpoint + " is served by " + handlers);
            }
        });
        assertTrue(clashes.isEmpty(), String.join(System.lineSeparator(), clashes));
    }
}
