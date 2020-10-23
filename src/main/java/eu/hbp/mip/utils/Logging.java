package eu.hbp.mip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Logging {

    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);

    public static void LogUserAction(String userName, String endpoint, String actionInfo) {
        LOGGER.info(" User -> " + userName + " ,"
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
