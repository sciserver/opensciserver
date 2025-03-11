package org.sciserver.springapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * In the case where prefetch is true, either authenticate with login-portal or check cache entry at start of each
 * request. This is useful if there are endpoints that don't need authentication to still populate logs with user info
 * (when using the logging interceptor).
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${auth.prefetch:true}") private boolean prefetch;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler
    ) throws Exception {
        if (prefetch) {
            Auth.prefetch();
        }
        return true;
    }

}
