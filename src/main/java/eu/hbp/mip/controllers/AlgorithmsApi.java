package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.google.gson.Gson;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.model.AlgorithmDTO;
import eu.hbp.mip.model.galaxy.WorkflowDTO;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.UserActionLogging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/algorithms", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/algorithms")
public class AlgorithmsApi {

    private static final Gson gson = new Gson();

    @Value("#{'${services.exareme.algorithmsUrl}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @ApiOperation(value = "List all algorithms", response = String.class)
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<AlgorithmDTO>> getAlgorithms() {
        UserActionLogging.LogAction("List all algorithms", "");

        List<AlgorithmDTO> exaremeAlgorithms = getExaremeAlgorithms();
        List<AlgorithmDTO> galaxyAlgorithms = getGalaxyWorkflows();

        List<AlgorithmDTO> algorithms = new LinkedList<>();
        if (exaremeAlgorithms != null) {
            algorithms.addAll(exaremeAlgorithms);
        } else {
            UserActionLogging.LogAction("List all algorithms",
                    "Getting exareme algorithms failed and returned null");
        }
        if (galaxyAlgorithms != null) {
            algorithms.addAll(galaxyAlgorithms);
        } else {
            UserActionLogging.LogAction("List all algorithms",
                    "Getting galaxy workflows failed and returned null");
        }

        return ResponseEntity.ok(algorithms);
    }

    /**
     * This method gets all the available exareme algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public List<AlgorithmDTO> getExaremeAlgorithms() {
        UserActionLogging.LogAction("List exareme algorithms", "");

        List<AlgorithmDTO> algorithms = new LinkedList<>();
        // Get exareme algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaremeAlgorithmsUrl, response);

            algorithms = gson.fromJson(response.toString(), algorithms.getClass());
        } catch (IOException e) {
            UserActionLogging.LogAction("List exareme algorithms", "An exception occurred: " + e.getMessage());
            return null;
        }

        UserActionLogging.LogAction("List exareme algorithms",
                "Completed, returned " + algorithms.size() + " algorithms.");
        return algorithms;
    }

    /**
     * This method gets all the available galaxy workflows, converts them into algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public List<AlgorithmDTO> getGalaxyWorkflows() {
        UserActionLogging.LogAction("List Galaxy workflows", "");

        List<Workflow> workflowList = null;
        try {
            // Get all the workflows with the galaxy client
            final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
            final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

            workflowList = new ArrayList<>(workflowsClient.getWorkflows());
        } catch (Exception e) {
            UserActionLogging.LogAction("List Galaxy workflows", "Error when calling list galaxy workflows: " + e.getMessage());

            return null;
        }

        // Get the workflow details with the custom client to receive them as a WorkflowDTO
        List<WorkflowDTO> workflows = new LinkedList<>();
        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        for (Workflow workflow : workflowList) {
            // Call Galaxy to run the workflow
            Call<WorkflowDTO> call = service.getWorkflowFromGalaxy(workflow.getId(), galaxyApiKey);
            try {
                Response<WorkflowDTO> response = call.execute();

                if (response.code() == 200) {       // Call succeeded
                    workflows.add(response.body());

                } else {     // Something unexpected happened
                    String msgErr = gson.toJson(response.errorBody());
                    UserActionLogging.LogAction("List Galaxy workflows", "Error Response: " + msgErr);
                    return null;
                }
            } catch (Exception e) {
                UserActionLogging.LogAction("List Galaxy workflows", "An exception occurred: " + e.getMessage());
                return null;
            }
        }
        UserActionLogging.LogAction("List Galaxy workflows", "Workflows fetched: " + workflows.size());

        // Convert the workflows to algorithms
        List<AlgorithmDTO> algorithms = new LinkedList<>();
        for (WorkflowDTO workflow : workflows) {
            UserActionLogging.LogAction("List Galaxy workflows", "Converting workflow: " + workflow);

            algorithms.add(workflow.convertToAlgorithmDTO());

            UserActionLogging.LogAction("List Galaxy workflows",
                    "Converted algorithm: " + algorithms.get(algorithms.size() - 1));
        }

        UserActionLogging.LogAction("List Galaxy workflows", "Completed!");
        return algorithms;
    }
}
