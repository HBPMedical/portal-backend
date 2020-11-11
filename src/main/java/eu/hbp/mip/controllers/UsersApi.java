package eu.hbp.mip.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.hbp.mip.model.DAOs.UserDAO;
import eu.hbp.mip.repositories.UserRepository;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/users", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/users")
public class UsersApi {

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private UserRepository userRepository;

    @ApiOperation(value = "Get a user", response = UserDAO.class)
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public ResponseEntity<UserDAO> getAUser(
            @ApiParam(value = "username", required = true) @PathVariable("username") String username
    ) {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /users/{username}",
                "Loaded a user with username : " + username);

        // TODO Error handling?
        return ResponseEntity.ok(userRepository.findByUsername(username));
    }

    @ApiOperation(value = "Get the active user", response = UserDAO.class)
    @RequestMapping(value = "/activeUser", method = RequestMethod.GET)
    public ResponseEntity<UserDAO> getTheActiveUser(HttpServletResponse response) {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /users/activeUser",
                "Loading the details of the activeUser");

        UserDAO activeUser = activeUserService.getActiveUser();

        // Add the active user to a cookie
        try {
            // TODO needed? Ask Manuel
            ObjectMapper mapper = new ObjectMapper();
            String userJSON = mapper.writeValueAsString(activeUser);
            Cookie cookie = new Cookie("user", URLEncoder.encode(userJSON, "UTF-8"));
            cookie.setSecure(true);
            cookie.setPath("/");
            response.addCookie(cookie);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            Logging.LogUserAction(activeUser.getUsername(),
                    "(GET) /users/activeUser", "Failed to add Cookie. Exception: " + e.getMessage());
        }

        return ResponseEntity.ok(activeUserService.getActiveUser());
    }

    // TODO Kostas, why not working?
    @ApiOperation(value = "The active user agrees to the NDA", response = UserDAO.class)
    @RequestMapping(value = "/activeUser/agreeNDA", method = RequestMethod.POST)
    public ResponseEntity<UserDAO> activeUserServiceAgreesToNDA(@RequestBody(required = false) UserDAO userDAO) {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /users/activeUser/agreeNDA",
                "The user agreed to the NDA");

        return ResponseEntity.ok(activeUserService.agreeToNDA());
    }
}
