package hbp.mip.user;

import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/activeUser", produces = {APPLICATION_JSON_VALUE})
public class ActiveUserAPI {

    private final ActiveUserService activeUserService;

    public ActiveUserAPI(ActiveUserService activeUserService) {
        this.activeUserService = activeUserService;
    }

    @GetMapping
    public ResponseEntity<UserDTO> getTheActiveUser(Authentication authentication) {
        var user = activeUserService.getActiveUser(authentication);

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "GET",
                "endpoint", "/activeUser"
        );

        var logger = new Logger(user.username(), requestDetails);
        logger.info("HTTP request");

        logger.info("User details returned.", Map.of("response", Map.of("status", 200, "user", user.username())));
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/agreeNDA")
    public ResponseEntity<UserDTO> activeUserServiceAgreesToNDA(Authentication authentication) {
        var user = activeUserService.getActiveUser(authentication);

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "POST",
                "endpoint", "/activeUser/agreeNDA"
        );

        var logger = new Logger(user.username(), requestDetails);
        logger.info("HTTP request");

        var updatedUser = activeUserService.agreeToNDA(authentication);

        logger.info("User agreed to the NDA.", Map.of("response", Map.of("status", 200, "user", updatedUser.username())));
        return ResponseEntity.ok(updatedUser);
    }
}
