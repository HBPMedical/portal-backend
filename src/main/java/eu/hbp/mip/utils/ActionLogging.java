package eu.hbp.mip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ActionLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionLogging.class);
    private static Integer maxEndpointLen = 5;
    private static Integer maxInfoLen = 5;

    public static void LogUserAction(String userName, String endpoint, String actionInfo) {
        maxEndpointLen = (maxEndpointLen <  userName.length() ? userName.length() : maxEndpointLen);
        maxInfoLen = (maxInfoLen <  endpoint.length() ? endpoint.length() :  maxInfoLen);
        String endpointSpacing = String.format("%" + (maxEndpointLen - userName.length() + 2) + "s", "");
        String infoSpacing = String.format("%" + (maxInfoLen - endpoint.length() + 2) + "s", "");
        LOGGER.info(" User -> " + userName
                + endpointSpacing
                + ",  Endpoint -> " + endpoint
                + infoSpacing
                + ",  Info ->  " + actionInfo);
    }

    // Used from Threads because threads can't get userName.
    public static void LogExperimentAction(String experimentName, String actionInfo) {
        LOGGER.info(" Experiment -> "+ experimentName
                + "  ,  Info -> " + actionInfo);
    }

    // Used when a user is not authorised yet
    public static void LogAction(String actionName, String actionIdInfo) {
        LOGGER.info(" Action -> " + actionName
                + "  ,  Info -> " + actionIdInfo);
    }

}
