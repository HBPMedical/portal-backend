package hbp.mip.configurations;

import hbp.mip.utils.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationEventListener {

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        Logger logger = new Logger(username, "LOGGING IN");
        logger.info("User " + username + " has logged in successfully");

        // If you need to log additional details:
        authentication.getAuthorities().forEach(authority ->
                logger.info("User " + username + " has authority " + authority.getAuthority())
        );
    }
}
