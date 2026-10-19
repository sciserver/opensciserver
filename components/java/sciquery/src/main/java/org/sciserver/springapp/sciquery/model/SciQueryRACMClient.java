package org.sciserver.springapp.sciquery.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.clientutils.Client;

/**
 * Following the pattern laid out in the <a href="https://github.com/sciserver/sciserver-clients">sciserver-clients</a>
 * project, adds wrappers for methods found in the <a href="https://github.com/sciserver/resource-management">resource-management</a>
 * project.
 * 
 * <p><b>TODO:</b> Code here should be refactored to the sciserver-clients project once the MVP is complete.</p>
 */
public class SciQueryRACMClient extends Client<SciQueryRACMClientInterface> {
    private static final Logger LOG = LogManager.getLogger();

    public SciQueryRACMClient(String racmEndpoint) {
        this(racmEndpoint, null);
    }

    public SciQueryRACMClient(String racmEndpoint, String racmServiceToken) {
        super(racmEndpoint, racmServiceToken, SciQueryRACMClientInterface.class);
    }

    // PLACEHOLDER for methods under testing prior to move to sciserver-clients package
}
