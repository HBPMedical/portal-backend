package eu.hbp.mip.controllers;

import eu.hbp.mip.model.User;
import eu.hbp.mip.model.UserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import eu.hbp.mip.utils.ActionLogging;

import java.time.LocalDateTime;

/**
 * Created by mirco on 08.05.17.
 */

@RestController
@RequestMapping(value = "/protected")
@Api(value = "/protected", description = "the protected files API")
public class FilesAPI {


    @Autowired
    private UserInfo userInfo;

    @ApiOperation(value = "Get protected files")
    @RequestMapping(value = "/{filename:.+}", method = RequestMethod.GET)
    public ResponseEntity<Void> getProtectedFile(
            @ApiParam(value = "filename", required = true) @PathVariable("filename") String filename
    ) {
        User user = userInfo.getUser();
        ActionLogging.LogUserAction(user.getUsername(), "(GET) /protected/{filename:.+}", "Loading protected file with filename : " + filename);

        String filepath = "/protected/" + filename;
        ActionLogging.LogUserAction(user.getUsername(), "(GET) /protected/{filename:.+}" + filepath, "Downloaded protected file");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Accel-Redirect", filepath);

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }
}
