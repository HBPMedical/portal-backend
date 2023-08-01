package eu.hbp.mip.controllers;

import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logger;
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
    public ResponseEntity<UserDAO> getTheActiveUser(Authentication authentication) {
        UserDAO activeUser = activeUserService.getActiveUser(authentication);
        Logger logger = new Logger(activeUser.getUsername(), "(GET) /activeUser");
        logger.LogUserAction("Loading the details of the activeUser");

        return ResponseEntity.ok(activeUser);
    }

    @PostMapping(value = "/agreeNDA")
    public ResponseEntity<UserDAO> activeUserServiceAgreesToNDA(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).getUsername(), "(GET) /activeUser/agreeNDA");
        logger.LogUserAction("The user agreed to the NDA");
        return ResponseEntity.ok(activeUserService.agreeToNDA(authentication));
    }
}
