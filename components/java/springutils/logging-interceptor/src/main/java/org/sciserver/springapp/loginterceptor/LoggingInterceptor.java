package org.sciserver.springapp.loginterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import sciserver.logging.Logger;
import sciserver.logging.ServiceLog;


/**
 * Intercept logging and add request information such as http return code, user info, error info, call method.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Autowired
    private ServiceLog serviceLog;

    @Autowired
    private Logger logger;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler
    ) throws Exception {
        serviceLog.addRequestInfo(request);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object
                                 handler, Exception exception
    ) throws Exception {
        if (!serviceLog.isCompleted()) {
            // add auth info if present
            String userid = (String) request.getAttribute("sciserver.auth.userId");
            if (userid != null) {
                serviceLog.setAttr("userId", userid);
            }
            String userName = (String) request.getAttribute("sciserver.auth.userName");
            if (userName != null) {
                serviceLog.setAttr("userName", userName);
            }
            if (exception != null) {
                serviceLog.setAttr("status", "500");
                serviceLog.setAttr("error", exception.toString());
                StringWriter stackTraceString = new StringWriter();
                exception.printStackTrace(new PrintWriter(stackTraceString));
                serviceLog.setAttr("stackTrace", stackTraceString.toString());
            } else {
                serviceLog.setAttr("status", String.valueOf(response.getStatus()));
            }
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                serviceLog.setMethod(
                    String.format("%s.%s", handlerMethod.getBeanType().getName(), handlerMethod.getMethod().getName()
                    ));
            }
            logger.SendMessage(serviceLog);
        }
    }
}
