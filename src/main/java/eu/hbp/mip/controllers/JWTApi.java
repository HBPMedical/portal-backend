package eu.hbp.mip.controllers;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import eu.hbp.mip.model.User;
import eu.hbp.mip.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import eu.hbp.mip.utils.JWTUtil;
import eu.hbp.mip.utils.UserActionLogging;

@RestController
@RequestMapping(value = "/jwt", produces = { TEXT_PLAIN_VALUE })
@Api(value = "/jwt", description = "the jwt API")
public class JWTApi {

    @Autowired
    private UserInfo userInfo;

    @Value("#{'${services.workflows.jwtSecret}'}")
    private String jwtSecret;

    @ApiOperation(value = "Create a JSON Web Token", response = String.class)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createJWT() {

        UserActionLogging.LogAction("Create a JSON Web Token", "");

        User user = userInfo.getUser();
        String token = JWTUtil.getJWT(jwtSecret, user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}
