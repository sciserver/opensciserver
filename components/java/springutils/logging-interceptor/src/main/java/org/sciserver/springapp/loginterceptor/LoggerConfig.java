package org.sciserver.springapp.loginterceptor;

import java.io.FileNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import sciserver.logging.Logger;


/**
 * Configure the logger instance. See sciserver-logging-java for details about the logger.
 */
@Component
public class LoggerConfig {

    /**
     * Setup logger with optional Console, File, and remote queue output.
     *
     * @param app Application name for logs
     * @param rabbitQueue Remote queue name
     * @param rabbitExchange Remote exchange name
     * @param rabbitHost Remote host
     * @param rabbitPort Remote port (if non-default)
     * @param fileName When specified, path to file to write logs to
     * @param console When true, emit logs to console
     * @return logger instance
     *
     * @throws FileNotFoundException if the fileName given cannot be opened.
     */
    @Bean
    public Logger logger(
        @Value("${logging.application:unspecified}") String app,
        @Value("${logging.rabbitmq.queuename:#{null}}") String rabbitQueue,
        @Value("${logging.rabbitmq.exchange:#{null}}") String rabbitExchange,
        @Value("${logging.rabbitmq.host:#{null}}") String rabbitHost,
        @Value("${logging.rabbitmq.port:-1}") int rabbitPort,
        @Value("${logging.file.name:#{null}}") String fileName,
        @Value("${logging.console:false}") String console
    ) throws FileNotFoundException {
        Logger logger = new Logger();
        logger.applicationName = app;
        logger.remoteEnabled = false;
        if (rabbitHost != null) {
            logger.enableRabbitMq(rabbitHost, rabbitPort, rabbitExchange, rabbitQueue);
        }
        if (fileName != null) {
            logger.enableFileOutput(fileName);
        }
        if (console.equals("true")) {
            logger.enableConsoleOutput();
        }
        return logger;
    }

}
