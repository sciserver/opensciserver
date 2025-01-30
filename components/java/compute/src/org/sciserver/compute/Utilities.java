/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.core.container.ExecutableManager;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.DomainType;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.ExecutableImage;
import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.PublicVolume;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.core.registry.VolumeContainer;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import sciserver.logging.Message;


public class Utilities {
    private static final String COOKIE_NAME = "computeCookie";

    // Misc.
    public static String getStackTrace(Exception ex) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        ex.printStackTrace(writer);
        writer.close();

        return buffer.toString();
    }

    public static void retryContainerAction(int retries, long delay,
            ExecutableContainer container, ContainerAction a) throws Exception {
        while (true) {
            try {
                a.execute(container);
                return;
            } catch (Exception e) {
                if (--retries <= 0) {
                    throw e;
                }
                Thread.sleep(delay);
            }
        }
    }

    // HttpClient
    public static void ensureSuccessStatusCode(CloseableHttpResponse res) throws Exception {
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new Exception(res.getStatusLine().toString());
        }
    }

    public static void renderHealth(JspWriter out, HttpServletResponse response,
            boolean includeDisabledNodes) throws Exception {
        int statusCode = HttpStatus.SC_OK;
        AppConfig appConfig = AppConfig.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        Iterable<Domain> domains = appConfig.getRegistry().getDomains();
        for (Domain domain : domains) {
            sb.append("<tr><th colspan=\"3\">");
            sb.append(domain.getName());
            sb.append("</th></tr>\n");

            Iterable<Node> nodes = appConfig.getRegistry().getNodes(domain);
            for (Node node : nodes) {
                sb.append("<tr><td rowspan=\"2\" class=\"node-name\">");
                sb.append(node.getName());
                sb.append("</td>");
                String statusClass = "status-disabled";
                if (!node.isEnabled() && !includeDisabledNodes) {
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Containers: n/a</td></tr>\n");
                    sb.append("<tr>");
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Proxy: n/a</td></tr>\n");
                    continue;
                }
                
                try {
                    JsonNode jsonDocker = node.getDockerApiUrl() == null ? null : node.getStatus();
                    if (node.isEnabled()) {
                        statusClass = "status-ok";
                    }
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Containers: ");
                    sb.append(jsonDocker == null ? "n/a" : jsonDocker.at("/Containers").asInt());
                    sb.append(" / Total slots: " + node.getTotalSlots() + " / Used slots: " + node.getUsedSlots());
                    sb.append("</td>");
                } catch (Exception e) {
                    if (node.isEnabled()) {
                        statusClass = "status-error";
                        statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    }
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Docker error: ");
                    sb.append("<pre>" + e.toString() + "</pre>");
                    sb.append("</td>");
                }
                sb.append("</tr>\n");

                sb.append("<tr>");
                try {
                    JsonNode jsonRoutes = node.getProxyApiUrl() == null ? null : node.getProxyRoutes(new Date(0));
                    if (node.isEnabled()) {
                        statusClass = "status-ok";
                    }
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Proxy"
                            + (jsonRoutes == null ? ": n/a" : " OK") + "</td>");
                } catch (Exception e) {
                    if (node.isEnabled()) {
                        statusClass = "status-error";
                        statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    }
                    sb.append("<td class=\"" + statusClass + "\"></td><td>Proxy error: ");
                    sb.append("<pre>" + e.toString() + "</pre>");
                    sb.append("</td>");
                }
                sb.append("</tr>\n");
            }
        }
        sb.append("</table>");
        out.print(sb.toString());
        if (statusCode != HttpStatus.SC_OK) {
            response.setStatus(statusCode);
        }
    }

    // Container
    public static void injectToken(ExecutableContainer container, String token) throws Exception {
        ExecutableManager manager = (ExecutableManager) AppConfig.getInstance().getRegistry()
                .createContainerManager(container.getImage());
        manager.injectToken(container, token);
    }

    // HTTP
    public static String getToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String tokenFromHeader = request.getHeader("X-Auth-Token");
        if (tokenFromHeader != null) {
            return tokenFromHeader;
        }
        String tokenFromRequest = request.getParameter("token");
        if (tokenFromRequest != null) {
            setCookie(response, tokenFromRequest);
            return tokenFromRequest;
        }

        String tokenFromCookie = getCookieValue(request);
        if (tokenFromCookie != null) {
            return tokenFromCookie;
        } else {
            deleteCookie(response);
        }

        return null;
    }

    public static void setCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public static String getCookieValue(HttpServletRequest request) {
        String value = null;

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            cookies = new Cookie[0];
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                value = cookie.getValue();
            }
        }

        return value;
    }

    public static String getVolumeListAsString(ExecutableContainer container) throws Exception {
        StringBuilder attachedVolumes = new StringBuilder();
        for (VolumeContainer volumeContainer : container.getUserVolumes()) {
            if (attachedVolumes.length() > 0) {
                attachedVolumes.append("; ");
            }
            attachedVolumes.append(volumeContainer.getImage().getName());
        }
        for (PublicVolume publicVolume : container.getPublicVolumes()) {
            if (attachedVolumes.length() > 0) {
                attachedVolumes.append("; ");
            }
            attachedVolumes.append(publicVolume.getName());
        }
        return attachedVolumes.toString();
    }

    public static String getDisplayName(String name) {
        if (name == null || name.isEmpty()) {
            return "<None>";
        } else {
            return name;
        }
    }

    public static void fillLoggingMessage(Message message, AuthenticatedUser user,
            HttpServletRequest request, String taskName) {
        //setting user info
        try {
            //getting client IP address
            String clientIP = "";
            if (request.getHeader("X-FORWARDED-FOR") != null) {
                clientIP = request.getHeader("X-FORWARDED-FOR");
            } else if (request.getHeader("X-Forwarded-For") != null) {
                clientIP = request.getHeader("X-Forwarded-For");
            } else if (request.getHeader("HTTP_CLIENT_IP") != null) {
                clientIP = request.getHeader("HTTP_CLIENT_IP");
            } else {
                clientIP = request.getRemoteAddr();
            }
            message.ClientIP = clientIP.split(",")[0];

            //setting TaskName
            message.TaskName = request.getParameter("TaskName") != null ? request.getParameter("TaskName") : taskName;

            //user info
            if (user != null) {
                message.UserId = user.getUserId();
                message.UserName = user.getUserName();
            } else {
                message.UserId = "_SERVICE_";
                message.UserName = "_SERVICE_";
            }
        } catch (Exception e) {
            //Do nothing
        }
    }

    public static String structuredMessage(AuthenticatedUser user, String verb,
            ExecutableContainer container, ExecutableImage image,
            List<VolumeContainerModel> requestedVolumes, List<VolumeImage> userVolumeImages) throws Exception {

        String userName = "_SERVICE_";
        if (user != null) {
            userName = user.getUserName();
        }

        String volumesString = Stream.concat(
                requestedVolumes.stream().map(x -> "'" + x.getName() + "'"),
                userVolumeImages.stream().map(y -> "'" + y.getName() + "'"))
                .collect(Collectors.joining(", "));

        String predicate = String.format("Docker container '%s' from image '%s' with volumes %s",
                Utilities.getDisplayName(container.getName()),
                image.getName(),
                volumesString);

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode message = jsonFactory.objectNode();

        message.putObject("sentence")
                .put("subject", userName)
                .put("verb", verb)
                .put("predicate", predicate);

        message.put("image", image.getName());
        message.put("containerUUID", container.getExternalRef());

        message.set("domain", jsonFactory.objectNode()
                .put("name", image.getDomain().getName())
                .put("id", image.getDomainId()));

        message.putArray("dataVolumes").addAll(requestedVolumes.stream().map(x ->
            jsonFactory.objectNode()
                .put("name", x.getName())
                .put("needsWriteAccess", x.isWritable()))
            .collect(Collectors.toList()));

        message.putArray("userVolumes").addAll(userVolumeImages.stream().map(x ->
            jsonFactory.objectNode().put("name", x.getName()))
            .collect(Collectors.toList()));


        return message.toString();
    }

    public static String structuredMessage(AuthenticatedUser user, String verb,
            ExecutableContainer container) throws Exception {
        String userName = "_SERVICE_";
        if (user != null) {
            userName = user.getUserName();
        }

        String predicate = String.format("Docker container '%s'",
                Utilities.getDisplayName(container.getName()));

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode message = jsonFactory.objectNode();

        message.putObject("sentence")
                .put("subject", userName)
                .put("verb", verb)
                .put("predicate", predicate);

        message.put("image", container.getImage().getName());
        message.put("containerUUID", container.getExternalRef());

        message.set("domain", jsonFactory.objectNode()
                .put("name", container.getImage().getDomain().getName())
                .put("id", container.getImage().getDomainId()));

        return message.toString();
    }

    public static String structuredMessage(AuthenticatedUser user, String verb, String predicate) {
        String userName = "_SERVICE_";
        if (user != null) {
            userName = user.getUserName();
        }

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode message = jsonFactory.objectNode();

        message.putObject("sentence")
                .put("subject", userName)
                .put("verb", verb)
                .put("predicate", predicate);

        return message.toString();
    }

    public static List<UserDockerComputeDomainModel> getDaskDomains(String token) throws Exception {
        AppConfig appConfig = AppConfig.getInstance();
        Registry reg = appConfig.getRegistry();

        List<UserDockerComputeDomainModel> domains = appConfig.getRACMClient().getUserComputeDomainsInteractive(token)
                .stream()
                .filter(d -> {
                    try {
                        return
                                DomainType.DASK.equals(reg.getDomain(Long.parseLong(d.getPublisherDID())).getType())
                                && !d.getImages().isEmpty();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        return domains;
    }
}
