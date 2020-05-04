package eu.hbp.mip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserActionLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserActionLogging.class);

    public static void LogUserAction(String userName, String actionName, String actionInfo) {
        LOGGER.info(" User : "
                + userName
                + " called endpoint: " + actionName
                + ", info: " + actionInfo);
    }

    // Usually, used from Threads because threads can't get userName.
    // Also used when a user is not authorised yet
    public static void LogAction(String actionName, String actionIdInfo) {
        LOGGER.info("Action -->" + actionName + " info: " + actionIdInfo);
    }
}
