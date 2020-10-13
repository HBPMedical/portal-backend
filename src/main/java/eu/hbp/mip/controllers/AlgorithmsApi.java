package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.model.AlgorithmDTO;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.model.galaxy.WorkflowDTO;
import eu.hbp.mip.utils.CustomResourceLoader;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.ActionLogging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

import static eu.hbp.mip.utils.ErrorMessages.disabledAlgorithmsCouldNotBeLoaded;
import static eu.hbp.mip.utils.InputStreamConverter.convertInputStreamToString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/algorithms", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/algorithms")
public class AlgorithmsApi {

    private static final Gson gson = new Gson();

    @Autowired
    private UserInfo userInfo;

    @Value("#{'${services.exareme.algorithmsUrl}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @ApiOperation(value = "List all algorithms", response = String.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<AlgorithmDTO>> getAlgorithms() {
        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Executing...");

        LinkedList<AlgorithmDTO> exaremeAlgorithms = getExaremeAlgorithms();
        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Loaded "+ exaremeAlgorithms.size() +" exareme algorithms");
        LinkedList<AlgorithmDTO> galaxyAlgorithms = getGalaxyWorkflows();
        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Loaded "+ galaxyAlgorithms.size() +" galaxy algorithms");

        LinkedList<AlgorithmDTO> algorithms = new LinkedList<>();
        if (exaremeAlgorithms != null) {
            algorithms.addAll(exaremeAlgorithms);
        } else {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                    "Getting exareme algorithms failed and returned null");
        }
        if (galaxyAlgorithms != null) {
            algorithms.addAll(galaxyAlgorithms);
        } else {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                    "Getting galaxy workflows failed and returned null");
        }

        List<String> disabledAlgorithms = new ArrayList<>();
        try {
            disabledAlgorithms = getDisabledAlgorithms();
        } catch (IOException e) {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                    disabledAlgorithmsCouldNotBeLoaded);
        }

        // Remove any disabled algorithm
        LinkedList<AlgorithmDTO> allowedAlgorithms = new LinkedList<>();
        for (AlgorithmDTO algorithm : algorithms) {
            if (!disabledAlgorithms.contains(algorithm.getName())) {
                allowedAlgorithms.add(algorithm);
            }
        }
        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                "Successfully listed "+ allowedAlgorithms.size() +" algorithms");
        return ResponseEntity.ok(allowedAlgorithms);
    }

    /**
     * This method gets all the available exareme algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public LinkedList<AlgorithmDTO> getExaremeAlgorithms() {
        LinkedList<AlgorithmDTO> algorithms = new LinkedList<>();
        // Get exareme algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaremeAlgorithmsUrl, response);

            algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<LinkedList<AlgorithmDTO>>() {
                    }.getType()
            );
        } catch (IOException e) {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "An exception occurred: " + e.getMessage());
            return null;
        }

        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                "Completed, returned " + algorithms.size() + " algorithms.");
        return algorithms;
    }

    /**
     * This method gets all the available galaxy workflows, converts them into algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public LinkedList<AlgorithmDTO> getGalaxyWorkflows() {

        List<Workflow> workflowList;
        try {
            // Get all the workflows with the galaxy client
            final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
            final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

            workflowList = new ArrayList<>(workflowsClient.getWorkflows());
        } catch (Exception e) {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Error when calling list galaxy workflows: " + e.getMessage());

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
                    ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Error Response: " + msgErr);
                    return null;
                }
            } catch (Exception e) {
                ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "An exception occurred: " + e.getMessage());
                return null;
            }
        }
        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Workflows fetched: " + workflows.size());

        // Convert the workflows to algorithms
        LinkedList<AlgorithmDTO> algorithms = new LinkedList<>();
        for (WorkflowDTO workflow : workflows) {
            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Converting workflow: " + workflow);

            algorithms.add(workflow.convertToAlgorithmDTO());

            ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms",
                    "Converted algorithm: " + algorithms.get(algorithms.size() - 1));
        }

        ActionLogging.LogUserAction(userInfo.getUser().getUsername() , "(GET) /algorithms", "Completed!");
        return algorithms;
    }

    @Autowired
    private CustomResourceLoader resourceLoader;

    /**
     * Fetches the disabled algorithms from a .json file
     *
     * @return a list with their names
     * @throws IOException when the file could not be loaded
     */
    List<String> getDisabledAlgorithms() throws IOException {

        Resource resource = resourceLoader.getResource("file:/opt/portal/api/disabledAlgorithms.json");

        List<String> response = gson.fromJson(convertInputStreamToString(
                resource.getInputStream()),
                new TypeToken<List<String>>() {
                }.getType()
        );
        return response;
    }
}
