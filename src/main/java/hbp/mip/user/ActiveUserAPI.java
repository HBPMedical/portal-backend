package hbp.mip.user;

import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        UserDTO activeUser = activeUserService.getActiveUser(authentication);
        Logger logger = new Logger(activeUser.username(), "(GET) /activeUser");
        logger.info("User details returned.");
        return ResponseEntity.ok(activeUser);
    }

    @PostMapping(value = "/agreeNDA")
    public ResponseEntity<UserDTO> activeUserServiceAgreesToNDA(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /activeUser/agreeNDA");
        logger.info("User agreed to the NDA.");
        return ResponseEntity.ok(activeUserService.agreeToNDA(authentication));
    }
}
