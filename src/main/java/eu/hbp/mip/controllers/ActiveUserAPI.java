package eu.hbp.mip.controllers;

import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/activeUser", produces = {APPLICATION_JSON_VALUE})
public class ActiveUserAPI {

    private final ActiveUserService activeUserService;

    public ActiveUserAPI(ActiveUserService activeUserService) {
        this.activeUserService = activeUserService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<UserDAO> getTheActiveUser() {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /activeUser");
        logger.LogUserAction("Loading the details of the activeUser");

        return ResponseEntity.ok(activeUserService.getActiveUser());
    }

    @RequestMapping(value = "/agreeNDA", method = RequestMethod.POST)
    public ResponseEntity<UserDAO> activeUserServiceAgreesToNDA() {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /activeUser/agreeNDA");
        logger.LogUserAction("The user agreed to the NDA");
        return ResponseEntity.ok(activeUserService.agreeToNDA());
    }
}
