package hbp.mip.utils;

import org.slf4j.LoggerFactory;


public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    private final String username;
    private final String endpoint;

    public Logger(String username, String endpoint) {
        this.username = username;
        this.endpoint = endpoint;
    }

    public void LogUserAction(String actionInfo) {
        LOGGER.info(" User -> " + username + " ,"
                + "Endpoint -> " + endpoint + " ,"
                + "Info ->  " + actionInfo);
    }
}
