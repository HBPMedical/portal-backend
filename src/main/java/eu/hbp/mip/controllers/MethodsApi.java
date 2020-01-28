package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.google.gson.*;
import com.sun.jersey.api.client.ClientHandlerException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.utils.HTTPUtil;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.beans.factory.annotation.Autowired;
import eu.hbp.mip.utils.UserActionLogging;

@RestController
@RequestMapping(value = "/methods", produces = { APPLICATION_JSON_VALUE })
@Api(value = "/methods")
public class MethodsApi {

    private static final Gson gson = new Gson();

    @Value("#{'${services.exareme.algorithmsUrl:http://localhost:9090/mining/algorithms.json}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

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
            final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
            final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

            List<Workflow> workflowList = new ArrayList<>();
            for(Workflow workflow : workflowsClient.getWorkflows()) {
                workflowList.add(workflow);
            }

            List<WorkflowDetails> workflowDetailsList = new ArrayList<>();
            for(Workflow workflow : workflowList){
                workflowDetailsList.add(workflowsClient.showWorkflow(workflow.getId()));
            }

            return ResponseEntity.ok(workflowDetailsList);
        } catch (ClientHandlerException e) {
            return ResponseEntity.ok(new ArrayList<>());
        }

    }

}
