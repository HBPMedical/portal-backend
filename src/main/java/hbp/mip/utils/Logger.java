package hbp.mip.utils;

import org.apache.logging.log4j.LoggingException;
import org.slf4j.LoggerFactory;


public class Logger {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);
    private final String username;
    private final String endpoint;

    public Logger(String username, String endpoint) {
        this.username = username;
        this.endpoint = endpoint;
    }

    private void logUserAction(String message, String logLevel){
        String logMessage = " User -> " + username + " , " + "Endpoint -> " + endpoint + " , " + "Info ->  " + message;

        switch (logLevel) {
            case "ERROR" -> logger.error(logMessage);
            case "WARNING" -> logger.warn(logMessage);
            case "INFO" -> logger.info(logMessage);
            case "DEBUG" -> logger.debug(logMessage);
            default -> throw new LoggingException("Not supported loglevel: " + logLevel);
        }
    }

    public void error(String message) {
        logUserAction(message, "ERROR");
    }

    public void warn(String message) {
        logUserAction(message, "WARNING");
    }

    public void info(String message) {
        logUserAction(message, "INFO");
    }

    public void debug(String message) {
        logUserAction(message, "DEBUG");
    }
}
