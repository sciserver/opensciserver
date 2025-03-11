package org.sciserver.springapp.loginterceptor;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import sciserver.logging.Logger;
import sciserver.logging.ServiceLog;

/**
 * Log interface for spring boot applications. Each request gets a ServiceLog type log setup upon entry. This can be
 * obtained and added to using Log.get(). For example, to add an attribute in a controller:
 *   `Log.get().setAttr("infoaboutrequest", "theinfo")`
 */
@Service
public class Log implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * Get this requests service log, for adding timing and attributes particular to this request.
     *
     * @return ServicLog object for the current request.
     */
    public static ServiceLog get() {
        return context.getBean(ServiceLog.class);
    }

    /**
     * Get the logger singleton. Can be used to emit general log messages other than request service logs.
     *
     * @return global logger instance
     */
    public static Logger getLogger() {
        return context.getBean(Logger.class);
    }

}
