package org.sciserver.springapp.loginterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sciserver.logging.Logger;
import sciserver.logging.ServiceLog;


/**
 * Bean and interceptor setup for logging.
 */
@Component
public class LoggingInterceptorAppConfig implements WebMvcConfigurer {

    @Autowired
    LoggingInterceptor loggingInterceptor;

    @Autowired
    Logger logger;

    @Bean
    @RequestScope
    public ServiceLog serviceLog() {
        return logger.createServiceLog();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }
}
