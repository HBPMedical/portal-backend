package eu.hbp.mip.utils;

import org.slf4j.LoggerFactory;

import java.util.UUID;


public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    private  String username;
    private  String endpoint;

    public Logger(String username, String endpoint){
        this.username = username;
        this.endpoint = endpoint;
    }

    public void LogUserAction(String actionInfo) {
        LOGGER.info(" User -> " + username + " ,"
                + "Endpoint -> " + endpoint + " ,"
                + "Info ->  " + actionInfo);
    }

    // Used from Threads because threads can't get userName.
    public static void LogExperimentAction(String experimentName, UUID experimentId, String actionInfo) {
        LOGGER.info(" Experiment -> " + experimentName
                + "(" + experimentId + ") ,"
                + "Info -> " + actionInfo);
    }
}
