package org.sciserver.springapp.fileservice;

import org.sciserver.springapp.loginterceptor.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LogApplicationNameInject {
    @Autowired
    private Config config;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Log.getLogger().setApplicationName(config.getFileService().getName());
    }

}
