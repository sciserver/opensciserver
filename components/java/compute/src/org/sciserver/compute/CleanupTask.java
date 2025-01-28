/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.Node;

import com.fasterxml.jackson.databind.JsonNode;

public class CleanupTask extends TimerTask {
	private static final Logger logger = LogManager.getLogger(CleanupTask.class);
		
	@Override
	public void run() {
		AppConfig appConfig = AppConfig.getInstance();
		AppSettings appSettings = appConfig.getAppSettings();
		logger.info("Cleanup started");
		try {
			Date now = new Date();
			appConfig.setLastCleanup(now);
			
			Iterable<Domain> domains = appSettings.getRegistry().getDomains();
			for (Domain domain : domains) {
				try {
					Iterable<Node> nodes = appSettings.getRegistry().getNodes(domain);
					for (Node node : nodes) {
						try {
							Calendar cal = Calendar.getInstance();
							cal.setTime(now);
							cal.add(Calendar.HOUR_OF_DAY, -appSettings.getCleanupInactiveHours());

							JsonNode routes = node.getProxyRoutes(cal.getTime());
							Iterator<JsonNode> i = routes.elements();
							while (i.hasNext()) {
								JsonNode route = i.next();
								try {
									ExecutableContainer c = (ExecutableContainer)appSettings.getRegistry().getContainer((long) route.at("/container_id").asLong());
									c.stop();
								} catch (Exception ex) { }
							}
						} catch (Exception ex) { }
					}
				} catch (Exception ex) { }
			}
		} catch (Exception ex) { }
		logger.info("Cleanup finished");
	}

}
