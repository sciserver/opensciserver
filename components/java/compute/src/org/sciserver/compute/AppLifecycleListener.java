/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StreamUtils;


@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger logger = LogManager.getLogger(AppLifecycleListener.class);
    private AppConfig appConfig = AppConfig.getInstance();

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        logger.info("Context destroyed");
        appConfig.shutdown();
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        logger.info("Context initialized");
        ServletContext context = event.getServletContext();
        try {
            context.getSessionCookieConfig().setSecure(true);

            String propertiesFile = System.getProperty("sciserver.properties.file");
            InputStream propertiesInput;
            if (propertiesFile == null) {
                propertiesInput = context.getResourceAsStream("/WEB-INF/application.properties");
            } else {
                propertiesInput = new FileInputStream(propertiesFile);
            }
            appConfig.loadSettings(propertiesInput);

            InputStream versionInput = context.getResourceAsStream("/WEB-INF/version");
            String computeVersion = StreamUtils.copyToString(versionInput, Charset.defaultCharset());
            appConfig.setVersion(computeVersion);

            appConfig.startup();
        } catch (Exception ex) {
            logger.error("Error on Startup", ex);
            throw new RuntimeException(ex);
        }
    }
}
