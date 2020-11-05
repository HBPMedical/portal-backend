/*
 * Created by mirco on 14.01.16.
 */

package eu.hbp.mip.controllers;

import eu.hbp.mip.model.DAOs.UserDAO;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.repositories.UserRepository;
import eu.hbp.mip.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/users", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/users", description = "the users API")
public class UsersApi {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfo userInfo;

    @ApiOperation(value = "Get a user", response = UserDAO.class)
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public ResponseEntity<UserDAO> getAUser(
            @ApiParam(value = "username", required = true) @PathVariable("username") String username
    ) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /users/{username}", "Loaded a user with username : " + userInfo.getUser().getUsername());

        return ResponseEntity.ok(userRepository.findByUsername(username));
    }
}
