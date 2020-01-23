package eu.hbp.mip.controllers;

import com.google.gson.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import eu.hbp.mip.model.User;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.utils.HTTPUtil;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import eu.hbp.mip.utils.JWTUtil;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.beans.factory.annotation.Autowired;
import eu.hbp.mip.utils.UserActionLogging;

@RestController
@RequestMapping(value = "/methods", produces = { APPLICATION_JSON_VALUE })
@Api(value = "/methods", description = "the methods API")
public class MethodsApi {

    
    private static final Gson gson = new Gson();

    @Value("#{'${services.exareme.algorithmsUrl:http://localhost:9090/mining/algorithms.json}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.workflows.workflowUrl}'}")
    private String workflowUrl;

    @Value("#{'${services.workflows.jwtSecret}'}")
    private String jwtSecret;

    @Autowired
    private UserInfo userInfo;

    @ApiOperation(value = "List Exareme algorithms and validations", response = String.class)
    @RequestMapping(value = "/exareme", method = RequestMethod.GET)
    public ResponseEntity<Object> getExaremeAlgorithms() {
        UserActionLogging.LogAction("List Exareme algorithms and validations", "");

        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaremeAlgorithmsUrl, response);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

    @ApiOperation(value = "List Galaxy workflows", response = String.class)
    @RequestMapping(value = "/workflows", method = RequestMethod.GET)
    public ResponseEntity<Object> getWorkflows() {
        UserActionLogging.LogAction("List Galaxy workflows", "");

        try {
            User user = userInfo.getUser();
            String token = JWTUtil.getJWT(jwtSecret, user.getEmail());

            StringBuilder response = new StringBuilder();
            HTTPUtil.sendAuthorizedHTTP(workflowUrl + "/getAllWorkflowWithDetails", "", response, "GET", "Bearer " + token);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

}
