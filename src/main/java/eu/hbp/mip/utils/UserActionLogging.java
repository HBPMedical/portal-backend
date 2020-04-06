package eu.hbp.mip.utils;

import eu.hbp.mip.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserActionLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserActionLogging.class);

    public static void LogAction(String actionName, String actionIdInfo)
    {
        LOGGER.info( "Called endpoint: " + actionName
                + " info: " + actionIdInfo);
    }

    public static void LogUserAction(String userName, String actionName, String actionInfo)
    {
        LOGGER.info( " User : "
                + userName
                + " called endpoint: " + actionName
                + " info: " + actionInfo);
    }

    // Used from Threads because LogAction won't work.
    public static void LogThreadAction(String actionName, String actionIdInfo)
    {
        LOGGER.info( "Thread -->" + actionName + " info: " + actionIdInfo);
    } 
}
