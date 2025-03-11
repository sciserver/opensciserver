/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;

import java.time.Instant;
import java.util.Date;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.Registry;


public class InactivityCleanupTask extends TimerTask {
    private static final Logger logger = LogManager.getLogger(InactivityCleanupTask.class);
    private static String notebookDescription = "NOTEBOOK";

    private void cleanupContainers(Registry registry, int inactiveSeconds, String withDescription, boolean delete) {
        Date inactiveSince = Date.from(Instant.now().minusSeconds(inactiveSeconds));
        Iterable<ExecutableContainer> inactiveContainers;
        try {
            inactiveContainers = registry.getInactiveContainers(inactiveSince, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (ExecutableContainer container : inactiveContainers) {
            if (withDescription != null && !container.getDescription().equals(withDescription)) {
                continue;
            }
            try {
                if (delete) {
                    container.delete();
                    logger.info(String.format("inactive container %s deleted!", container.getId()));
                } else if (container.isRunning()) {
                    container.stop();
                    logger.info(String.format("inactive container %s stopped!", container.getId()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void expireContainers(Registry registry) {
        Iterable<ExecutableContainer> expiredContainers;
        try {
            expiredContainers = registry.getExpiredContainers();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (ExecutableContainer container : expiredContainers) {
            logger.info(String.format("deleting expired container %s!", container.getId()));
            try {
                container.delete();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    @Override
    public void run() {
        logger.info("inactivity cleanup task started");
        AppConfig appConfig = AppConfig.getInstance();
        AppSettings appSettings = appConfig.getAppSettings();
        Registry registry = appSettings.getRegistry();

        // interactive notebook session based activity cleanup (delete containers)
        logger.info("cleaning up inactive notebook session containers");
        cleanupContainers(registry, appSettings.getSessionInactiveTimeoutSecs(), notebookDescription, true);

        // coarse compute-ui-click based activity (stop containers)
        if (appSettings.getEnableDBBasedCleanup()) {
            logger.info("cleaning up inactive compute containers");
            try {
                int afterSeconds = 3600 * appSettings.getCleanupInactiveHours();
                cleanupContainers(registry, afterSeconds, null, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // expire based on domain max session time
        logger.info("cleaning up expired sessions");
        expireContainers(registry);

        logger.info("inactivity cleanup task finished");
    }

}
