package eu.hbp.mip.controllers;

import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/activeUser", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/activeUser")
public class ActiveUserAPI {

    @Autowired
    private ActiveUserService activeUserService;

    @ApiOperation(value = "Get the active user", response = UserDAO.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<UserDAO> getTheActiveUser(HttpServletResponse response) {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /activeUser",
                "Loading the details of the activeUser");

        return ResponseEntity.ok(activeUserService.getActiveUser());
    }

    @ApiOperation(value = "The active user agrees to the NDA", response = UserDAO.class)
    @RequestMapping(value = "/agreeNDA", method = RequestMethod.POST)
    public ResponseEntity<UserDAO> activeUserServiceAgreesToNDA(@RequestBody(required = false) UserDAO userDAO) {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /activeUser/agreeNDA",
                "The user agreed to the NDA");

        return ResponseEntity.ok(activeUserService.agreeToNDA());
    }
}
