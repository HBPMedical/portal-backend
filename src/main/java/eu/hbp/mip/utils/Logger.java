package eu.hbp.mip.utils;

import org.slf4j.LoggerFactory;

import java.util.UUID;


public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    private final String username;
    private final String endpoint;

    public Logger(String username, String endpoint){
        this.username = username;
        this.endpoint = endpoint;
    }

    public void LogUserAction(String actionInfo) {
        LOGGER.info(" User -> " + username + " ,"
                + "Endpoint -> " + endpoint + " ,"
                + "Info ->  " + actionInfo);
    }

    // Deprecated, should be removed
    public static void LogBackgroundAction(String experimentName, UUID experimentId, String actionInfo) {
        LOGGER.info(" Experiment -> " + experimentName
                + "(" + experimentId + ") ,"
                + "Info -> " + actionInfo);
    }
}
