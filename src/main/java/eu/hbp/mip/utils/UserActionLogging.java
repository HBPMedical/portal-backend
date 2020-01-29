package eu.hbp.mip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;

public class UserActionLogging {


    private static final Logger LOGGER = LoggerFactory.getLogger(UserActionLogging.class);

    public static void LogAction(String actionName, String actionIdInfo)
    {
        LOGGER.info( " User : "
                + SecurityContextHolder.getContext().getAuthentication().getName()
                + " called endpoint: " + actionName
                + " info: " + actionIdInfo);
    }

}
