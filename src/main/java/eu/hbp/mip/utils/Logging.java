package eu.hbp.mip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Logging {

    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);

    public static void LogUserAction(String userName, String endpoint, String actionInfo) {
        LOGGER.info(" User -> " + userName + " ,"
                + calculateAdditionalSpacing(userName.length(), 8)
                + "Endpoint -> " + endpoint + " ,"
                + calculateAdditionalSpacing(endpoint.length(), 32)
                + "Info ->  " + actionInfo);
    }

    // Used from Threads because threads can't get userName.
    public static void LogExperimentAction(String experimentName, UUID experimentId, String actionInfo) {
        LOGGER.info(" Experiment -> " + experimentName
                + "(" + experimentId + ") ,"
                + calculateAdditionalSpacing(experimentName.length() + experimentId.toString().length() + 2, 20)
                + "Info -> " + actionInfo);
    }

    // Used when a user is not authorised yet
    public static void LogAction(String actionName, String actionIdInfo) {
        LOGGER.info(" Action -> " + actionName + " ,"
                + calculateAdditionalSpacing(actionName.length() + 2, 20)
                + "Info -> " + actionIdInfo);
    }

    // Calculates the spacing that is needed to create consistent logs.
    private static String calculateAdditionalSpacing(Integer currentLen, Integer maxLen) {
        int additionalSpacing = (maxLen > currentLen ? maxLen - currentLen + 2 : 2);
        return String.format("%" + additionalSpacing + "s", "");
    }
}
