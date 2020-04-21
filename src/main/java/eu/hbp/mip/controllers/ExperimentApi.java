package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputDefinition;
import com.google.common.collect.Lists;
import com.google.gson.*;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.model.*;
import eu.hbp.mip.model.ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmExecutionParamDTO;
import eu.hbp.mip.model.galaxy.ErrorResponse;
import eu.hbp.mip.model.galaxy.GalaxyWorkflowResult;
import eu.hbp.mip.model.galaxy.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.repositories.ModelRepository;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.UserActionLogging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
    This api is creating experiments and running it's algorithm on the
    exareme or galaxy clients.
 */

@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/experiments")
public class ExperimentApi {

    private static final Gson gson = new Gson();

    @Autowired
    private UserInfo userInfo;

    private static final Gson gsonOnlyExposed = new GsonBuilder().serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").excludeFieldsWithoutExposeAnnotation().create();

    @Value("#{'${services.exareme.queryExaremeUrl}'}")
    private String queryExaremeUrl;

    @Value("#{'${services.workflows.workflowUrl}'}")
    private String workflowUrl;

    @Value("#{'${services.workflows.jwtSecret}'}")
    private String jwtSecret;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${hbp.authentication.enabled:1}'}")
    private boolean authenticationIsEnabled;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @ApiOperation(value = "get an experiment", response = Experiment.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public ResponseEntity<String> getExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        Experiment experiment;
        UUID experimentUuid;
        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Get Experiment", "Invalid Experiment UUID.");
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);

        if (experiment == null) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Get an experiment ", " uuid : " + uuid);

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }


    @ApiOperation(value = "Create an experiment", response = Experiment.class)
    @RequestMapping(value = "/runAlgorithm", method = RequestMethod.POST)
    public ResponseEntity<String> runExperiment(Authentication authentication, @RequestBody ExperimentExecutionDTO experimentExecutionDTO) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run algorithm", "Running the algorithm...");

        if(authenticationIsEnabled) {
            // --- Validating proper access rights on the datasets  ---
            List<String> userClaims = Arrays.asList(authentication.getAuthorities().toString().toLowerCase()
                    .replaceAll("[\\s+\\]\\[]", "").split(","));
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "User Claims", userClaims.toString());

            // Don't check for dataset claims if "super" claim exists allowing everything
            if (!userClaims.contains(ClaimUtils.allDatasetsAllowedClaim())) {
                // Getting the dataset from the experiment parameters
                String experimentDatasets = null;
                for (AlgorithmExecutionParamDTO parameter : experimentExecutionDTO.getAlgorithms().get(0).getParameters()) {
                    if (parameter.getName().equals("dataset")) {
                        experimentDatasets = parameter.getValue();
                        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run algorithm", "Found the dataset parameter!");
                        break;
                    }
                }

                if (experimentDatasets == null || experimentDatasets.equals("")) {
                    UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run algorithm",
                            "A dataset should be specified when running an algorithm.");
                    return ResponseEntity.badRequest().body("A dataset should be specified when running an algorithm.");
                }

                for (String dataset : experimentDatasets.split(",")) {
                    String datasetRole = ClaimUtils.getDatasetClaim(dataset);
                    if (!userClaims.contains(datasetRole.toLowerCase())) {
                        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run algorithm",
                                "You are not allowed to use dataset: " + dataset);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to use dataset: " + dataset);
                    }
                }
                UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run algorithm",
                        "User is authorized to use the datasets: " + experimentDatasets);
            }
        }

        // --- Run the experiment ---

        // Get the type of algorithm
        String algorithmType = experimentExecutionDTO.getAlgorithms().get(0).getType();

        // Run with the appropriate engine
        if (algorithmType.equals("workflow")) {
            return runGalaxyWorkflow(experimentExecutionDTO);
        } else {
            return runExaremeAlgorithm(experimentExecutionDTO);
        }
    }

    @ApiOperation(value = "Mark an experiment as viewed", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsViewed", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsViewed(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Mark an experiment as viewed", " uuid : " + uuid);

        Experiment experiment;
        UUID experimentUuid;
        User user = userInfo.getUser();
        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Mark an experiment as viewed", "Invalid Experiment UUID" + uuid);
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);
        if (!experiment.getCreatedBy().getUsername().equals(user.getUsername()))
            return new ResponseEntity<>("You're not the owner of this experiment", HttpStatus.BAD_REQUEST);
        experiment.setResultsViewed(true);
        experimentRepository.save(experiment);

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Experiment updated (marked as viewed)", " ");

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "Mark an experiment as shared", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsShared", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsShared(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Mark an experiment as shared", " uuid : " + uuid);

        return doMarkExperimentAsShared(uuid, true);
    }

    @ApiOperation(value = "Mark an experiment as unshared", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsUnshared", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsUnshared(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Mark an experiment as unshared", " uuid : " + uuid);

        return doMarkExperimentAsShared(uuid, false);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"maxResultCount"})
    public ResponseEntity<String> listExperiments(
            @ApiParam(value = "maxResultCount") @RequestParam int maxResultCount) {

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "List experiments", " maxResultCount : " + maxResultCount);

        return doListExperiments(false, null);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"slug", "maxResultCount"})
    public ResponseEntity<String> listExperiments(@ApiParam(value = "slug") @RequestParam("slug") String modelSlug,
                                                  @ApiParam(value = "maxResultCount") @RequestParam("maxResultCount") int maxResultCount) {

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "List experiments", " modelSlug : " + modelSlug);

        if (maxResultCount <= 0 && (modelSlug == null || "".equals(modelSlug))) {
            return new ResponseEntity<>("You must provide at least a slug or a limit of result",
                    HttpStatus.BAD_REQUEST);
        }

        return doListExperiments(false, modelSlug);
    }

    @ApiOperation(value = "list my experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"mine"})
    public ResponseEntity<String> listMyExperiments(Authentication authentication, @ApiParam(value = "mine") @RequestParam("mine") boolean mine) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "List my experiments", " mine : " + mine);

        return doListExperiments(true, null);
    }

    private ResponseEntity<String> doListExperiments(boolean mine, String modelSlug) {
        User user = userInfo.getUser();

        Iterable<Experiment> myExperiments = experimentRepository.findByCreatedBy(user);
        List<Experiment> expList = Lists.newLinkedList(myExperiments);
        if (!mine) {
            Iterable<Experiment> sharedExperiments = experimentRepository.findByShared(true);
            List<Experiment> sharedExpList = Lists.newLinkedList(sharedExperiments);
            expList.addAll(sharedExpList);
        }

        if (modelSlug != null && !"".equals(modelSlug)) {
            for (Iterator<Experiment> it = expList.iterator(); it.hasNext(); ) {
                Experiment e = it.next();
                e.setResult(null);
                e.setAlgorithms(null);
                e.setValidations(null);
                if (!e.getModel().getSlug().equals(modelSlug)) {
                    it.remove();
                }
            }
        }

        return new ResponseEntity<>(gsonOnlyExposed.toJson(expList), HttpStatus.OK);
    }

    private ResponseEntity<String> doMarkExperimentAsShared(String uuid, boolean shared) {
        Experiment experiment;
        UUID experimentUuid;
        User user = userInfo.getUser();
        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            //LOGGER.trace("Invalid UUID", iae);
            //LOGGER.warn("An invalid Experiment UUID was received !");
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);

        if (!experiment.getCreatedBy().getUsername().equals(user.getUsername()))
            return new ResponseEntity<>("You're not the owner of this experiment", HttpStatus.BAD_REQUEST);

        experiment.setShared(shared);
        experimentRepository.save(experiment);

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Experiment updated (marked as shared)", "");

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    /* -------------------------------  EXPERIMENT MODEL METHODS  ----------------------------------------------------*/

    public Experiment createExperiment(ExperimentExecutionDTO experimentExecutionDTO) {
        User user = userInfo.getUser();

        Experiment experiment = new Experiment();
        experiment.setUuid(UUID.randomUUID());
        experiment.setCreatedBy(user);
        experiment.setAlgorithms(gson.toJson(experimentExecutionDTO.getAlgorithms()));
        Model model = modelRepository.findOne(experimentExecutionDTO.getModel());
        experiment.setModel(model);
        experiment.setName(experimentExecutionDTO.getName());
        experimentRepository.save(experiment);

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Created an experiment", " id : " + experiment.getUuid());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Created an experiment", " algorithms : " + experiment.getAlgorithms());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Created an experiment", " model : " + experiment.getModel().getSlug());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Created an experiment", " name : " + experiment.getName());
        return experiment;
    }

    private void saveExperiment(Experiment experiment) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " id : " + experiment.getUuid());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " algorithms : " + experiment.getAlgorithms());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " model : " + experiment.getModel().getSlug());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " name : " + experiment.getName());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " historyId : " + experiment.getWorkflowHistoryId());
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Saved an experiment", " status : " + experiment.getWorkflowStatus());

        experimentRepository.save(experiment);

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Experiment saved", "");
    }

    private void finishExperiment(Experiment experiment) {
        experiment.setFinished(new Date());
        experimentRepository.save(experiment);

        UserActionLogging.LogAction("Experiment finished!", "");
    }

    /* --------------------------------------  EXAREME CALLS ---------------------------------------------------------*/

    /**
     * The runExaremeExperiment will POST the algorithm to the exareme client
     *
     * @param experimentExecutionDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ResponseEntity<String> runExaremeAlgorithm(ExperimentExecutionDTO experimentExecutionDTO) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run exareme algorithm", "Running the algorithm...");

        Experiment experiment = createExperiment(experimentExecutionDTO);

        // Run the 1st algorithm from the list
        String algorithmName = experimentExecutionDTO.getAlgorithms().get(0).getName();

        // Get the parameters
        List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmExecutionParamDTO> algorithmParameters
                = experimentExecutionDTO.getAlgorithms().get(0).getParameters();

        String body = gson.toJson(algorithmParameters);
        String url = queryExaremeUrl + "/" + algorithmName;
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run exareme algorithm", "url: " + url + ", body: " + body);

        ResponseEntity<String> response = new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run exareme algorithm",
                "Completed, returning: " + experiment.toString());

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run exareme algorithm",
                "Starting exareme execution thread");
        new Thread(() -> {
            // ATTENTION: Inside the Thread only LogAction should be used, not LogAction!
            UserActionLogging.LogAction("Run exareme algorithm",
                    "Thread started!");

            try {
                UserActionLogging.LogAction("Run exareme algorithm",
                        "Thread started!");
                StringBuilder results = new StringBuilder();
                int code = HTTPUtil.sendPost(url, body, results);

                UserActionLogging.LogAction("Run exareme algorithm",
                        "Algorithm finished with code: " + code);

                // Results are stored in the experiment object
                experiment.setResult("[" + results.toString() + "]");
                experiment.setHasError(code >= 400);
                experiment.setHasServerError(code >= 500);
            } catch (Exception e) {
                UserActionLogging.LogAction("Run exareme algorithm",
                        "There was an exception: " + e.getMessage());

                experiment.setHasError(true);
                experiment.setHasServerError(true);
                experiment.setResult(e.getMessage());
            }
            UserActionLogging.LogAction("Run exareme algorithm",
                    "Finished the experiment: " + experiment.toString());
            finishExperiment(experiment);

            UserActionLogging.LogAction("Run exareme algorithm",
                    "Finished!");
        }).start();

        return response;
    }

    /* ---------------------------------------  GALAXY CALLS ---------------------------------------------------------*/


    /**
     * The runWorkflow will POST the algorithm to the galaxy client
     *
     * @param experimentExecutionDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ResponseEntity<String> runGalaxyWorkflow(ExperimentExecutionDTO experimentExecutionDTO) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "Running a workflow...");

        Experiment experiment = createExperiment(experimentExecutionDTO);

        // Run the 1st algorithm from the list
        String workflowId = experimentExecutionDTO.getAlgorithms().get(0).getName();

        // Get the parameters
        List<AlgorithmExecutionParamDTO> algorithmParameters
                = experimentExecutionDTO.getAlgorithms().get(0).getParameters();

        // Convert the parameters to workflow parameters
        HashMap<String, String> algorithmParamsIncludingEmpty = new HashMap<>();
        if (algorithmParameters != null) {
            for (AlgorithmExecutionParamDTO param : algorithmParameters) {
                algorithmParamsIncludingEmpty.put(param.getName(), param.getValue());
            }
        }

        // Get all the algorithm parameters because the frontend provides only the non-null
        final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();
        Workflow workflow = null;
        for (Workflow curWorkflow : workflowsClient.getWorkflows()) {
            if (curWorkflow.getId().equals(workflowId)) {
                workflow = curWorkflow;
                break;
            }
        }
        if (workflow == null) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow",
                    "Could not find algorithm code: " + workflowId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Could not find galaxy algorithm.").toString());
        }
        final WorkflowDetails workflowDetails = workflowsClient.showWorkflow(workflow.getId());
        for (Map.Entry<String, WorkflowInputDefinition> workflowParameter : workflowDetails.getInputs().entrySet()) {
            if (!(algorithmParamsIncludingEmpty.containsKey(workflowParameter.getValue().getUuid()))) {
                algorithmParamsIncludingEmpty.put(workflowParameter.getValue().getUuid(), "");
            }
        }

        // Create the body of the request
        HashMap<String, HashMap<String, String>> requestBody = new HashMap<>();
        requestBody.put("inputs", algorithmParamsIncludingEmpty);
        JsonObject requestBodyJson = new JsonParser().parse(gson.toJson(requestBody)).getAsJsonObject();

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "Running Galaxy workflow with id: " + workflow.getId());

        // Call Galaxy to run the workflow
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(workflow.getId(), galaxyApiKey, requestBodyJson);
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();

            if (response.code() == 200) {       // Call succeeded
                String responseBody = gson.toJson(response.body());
                UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "Response: " + responseBody);

                String historyId = (String) new JSONObject(responseBody).get("history_id");
                experiment.setWorkflowHistoryId(historyId);
                experiment.setWorkflowStatus("running");
                experiment.setHasError(false);
                experiment.setHasServerError(response.code() >= 500);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "Error Response: " + msgErr);

                // Values are read from streams.
                JSONObject jObjectError = new JSONObject(msgErr);
                String errMsg = jObjectError.get("err_msg").toString();

                experiment.setResult("[" + new ErrorResponse(errMsg).toString() + "]");
                experiment.setHasError(response.code() >= 400);
                experiment.setHasServerError(response.code() >= 500);
            }

        } catch (Exception e) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "An exception occurred: " + e.getMessage());
            experiment.setHasError(true);
            experiment.setHasServerError(true);
            experiment.setResult(e.getMessage());
        }
        saveExperiment(experiment);

        // Start the process of fetching the status
        updateWorkflowExperiment(experiment);

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run workflow", "Run workflow completed!");

        return new ResponseEntity(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }


    /**
     * This method creates a thread that will fetch the workflow result when it is ready
     *
     * @param experiment will be used to fetch it's workflow status, it should have the workflowHistoryId initialized
     *                   and the result should not already be fetched
     * @return nothing, just updates the experiment
     */
    public void updateWorkflowExperiment(Experiment experiment) {

        if (experiment == null) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Update workflow experiment", "The experiment does not exist.");
            return;
        }

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Update workflow experiment",
                " Experiment id : " + experiment.getUuid());

        if (experiment.getWorkflowHistoryId() == null) {
            UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Update workflow experiment", "History Id does not exist.");
            return;
        }

        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Update workflow experiment", "Starting Thread...");
        new Thread(() -> {
            while (true) {
                // ATTENTION: Inside the Thread only LogAction should be used, not LogAction!
                UserActionLogging.LogAction("Update workflow experiment", "Thread is running...");

                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    UserActionLogging.LogAction("Update workflow experiment",
                            "Sleep was disrupted: " + e.getMessage());
                }

                UserActionLogging.LogAction("Update workflow experiment",
                        "Fetching status for experiment Id: " + experiment.getUuid());

                String state = getWorkflowStatus(experiment.getWorkflowHistoryId());
                UserActionLogging.LogAction("Update workflow experiment", "State is: " + state);

                switch (state) {
                    case "running":
                        // Do nothing, when the experiment is created the status is set to running
                        UserActionLogging.LogAction("Update workflow experiment",
                                "Workflow is still running.");
                        break;

                    case "completed":
                        // Get only the job result that is visible
                        List<GalaxyWorkflowResult> workflowJobsResults = getWorkflowResults(experiment.getWorkflowHistoryId());
                        UserActionLogging.LogAction("Update workflow experiment",
                                "Results are: " + workflowJobsResults.toString());

                        boolean resultFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getVisible()) {
                                UserActionLogging.LogAction("Update workflow experiment",
                                        "Visible result are: " + jobResult.getId());

                                String result = getWorkflowResultBody(experiment.getWorkflowHistoryId(), jobResult.getId());

                                UserActionLogging.LogAction("Update workflow experiment", "Result: " + result);
                                if (result == null) {
                                    experiment.setHasError(true);
                                    experiment.setHasServerError(true);
                                }
                                experiment.setResult("[" + result + "]");
                                experiment.setWorkflowStatus("completed");
                                resultFound = true;
                            }
                        }

                        if (!resultFound) {      // If there is no visible result
                            UserActionLogging.LogAction("Update workflow experiment", "No visible result");
                            experiment.setResult("[" + new ErrorResponse("The workflow has no visible result.").toString() + "]");
                            experiment.setHasError(true);
                            experiment.setHasServerError(true);
                        }

                        finishExperiment(experiment);
                        break;

                    case "error":
                        // Get the job result that failed
                        workflowJobsResults = getWorkflowResults(experiment.getWorkflowHistoryId());
                        UserActionLogging.LogAction("Update workflow experiment",
                                "Error results are: " + workflowJobsResults.toString());

                        boolean failedJobFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getState().equals("error")) {
                                UserActionLogging.LogAction("Update workflow experiment",
                                        "Failed job is: " + jobResult.getId());

                                String result = getWorkflowJobError(jobResult.getId());

                                UserActionLogging.LogAction("Update workflow experiment", "Job result: " + result);
                                if (result == null) {
                                    experiment.setHasError(true);
                                    experiment.setHasServerError(true);
                                }
                                experiment.setResult("[" + result + "]");
                                experiment.setWorkflowStatus("error");
                                failedJobFound = true;
                            }
                        }

                        if (!failedJobFound) {      // If there is no visible failed job
                            UserActionLogging.LogAction("Update workflow experiment", "No failed result");
                            experiment.setResult("[" + new ErrorResponse("The workflow has no failed result.").toString() + "]");
                            experiment.setHasError(true);
                            experiment.setHasServerError(true);
                        }
                        finishExperiment(experiment);
                        break;

                    default:        // InternalError or unexpected result
                        experiment.setResult("[" + new ErrorResponse("An unexpected error occurred.").toString() + "]");
                        experiment.setHasError(true);
                        experiment.setHasServerError(true);
                        finishExperiment(experiment);
                        break;
                }

                // If result exists return
                if (experiment.getResult() != null) {
                    UserActionLogging.LogAction("Update workflow experiment",
                            "Result exists: " + experiment.getResult());
                    return;
                }
            }
        }).start();
    }


    /**
     * @param historyId The historyId of the workflow
     * @return "running"           ->      When the workflow is still running
     * "internalError"     ->      When an exception or a bad request occurred
     * "error"             ->      When the workflow produced an error
     * "completed"         ->      When the workflow completed successfully
     */
    public String getWorkflowStatus(String historyId) {
        // ATTENTION: This function is used from a Thread. Only LogAction should be used, not LogAction!
        UserActionLogging.LogAction("Get workflow status", " History Id : " + historyId);

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(historyId, galaxyApiKey);

        String result = null;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                UserActionLogging.LogAction("Get workflow status", " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return "internalError";
            }
            result = new Gson().toJson(response.body());
            UserActionLogging.LogAction("Get workflow status", " Result: " + result);

        } catch (IOException e) {
            UserActionLogging.LogAction("Get workflow status"
                    , " An exception happened: " + e.getMessage());
            return "internalError";
        }

        String state = null;
        try {
            JSONObject resultJson = new JSONObject(result);
            state = resultJson.getString("state");
        } catch (JSONException e) {
            UserActionLogging.LogAction("Get workflow status"
                    , " An exception happened: " + e.getMessage());
            return "internalError";
        }

        UserActionLogging.LogAction("Get workflow status", " Completed!");
        switch (state) {
            case "ok":
                return "completed";
            case "error":
                return "error";
            case "running":
            case "new":
            case "waiting":
            case "queued":
                return "running";
            default:
                return "internalError";
        }
    }

    /**
     * @param historyId The historyId of the workflow
     * @return a List<GalaxyWorkflowResult>   or null when an error occurred
     */
    public List<GalaxyWorkflowResult> getWorkflowResults(String historyId) {
        UserActionLogging.LogAction("Get workflow results", " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<List<GalaxyWorkflowResult>> call = service.getWorkflowResultsFromGalaxy(historyId, galaxyApiKey);

        List<GalaxyWorkflowResult> getGalaxyWorkflowResultList = null;
        try {
            Response<List<GalaxyWorkflowResult>> response = call.execute();
            if (response.code() >= 400) {
                UserActionLogging.LogAction("Get workflow results", " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            getGalaxyWorkflowResultList = response.body();
            UserActionLogging.LogAction("Get workflow results", " Result: " + response.body());

        } catch (IOException e) {
            UserActionLogging.LogAction("Get workflow results"
                    , " An exception happened: " + e.getMessage());
            return null;
        }

        UserActionLogging.LogAction("Get workflow results", " Completed!");
        return getGalaxyWorkflowResultList;

    }

    /**
     * @param historyId the historyId of the workflow
     * @param contentId the id of the job result that we want
     * @return the result of the specific workflow job, null if there was an error
     */
    public String getWorkflowResultBody(String historyId, String contentId) {
        UserActionLogging.LogAction("Get workflow results Body", " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call =
                service.getWorkflowResultsBodyFromGalaxy(historyId, contentId, galaxyApiKey);

        String resultJson = null;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                UserActionLogging.LogAction("Get workflow results Body", " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            resultJson = new Gson().toJson(response.body());
            UserActionLogging.LogAction("Get workflow results Body", " Result: " + resultJson);

        } catch (IOException e) {
            UserActionLogging.LogAction("Get workflow results Body",
                    " An exception happened: " + e.getMessage());
            return null;
        }

        UserActionLogging.LogAction("Get workflow results Body", " Completed!");
        return resultJson;
    }


    /**
     * @param jobId the id of the workflow job that failed
     * @return the error that was produced or null if an error occurred
     */
    public String getWorkflowJobError(String jobId) {
        UserActionLogging.LogAction("Get workflow job error", " jobId : " + jobId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> callError = service.getErrorMessageOfWorkflowFromGalaxy(jobId, galaxyApiKey);

        String fullError = null;
        String returnError = null;
        try {
            Response<Object> response = callError.execute();
            if (response.code() >= 400) {
                UserActionLogging.LogAction("Get workflow job error", "Response code: "
                        + response.code() + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }

            // Parsing the stderr of the job that failed
            String jsonString = new Gson().toJson(response.body());
            JsonElement jsonElement = new JsonParser().parse(jsonString);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            fullError = rootObject.get("stderr").getAsString();
            UserActionLogging.LogAction("Get workflow job error", "Error: " + fullError);

            String[] arrOfStr = fullError.split("ValueError", 0);
            String specError = arrOfStr[arrOfStr.length - 1];
            returnError = specError.substring(1);
            UserActionLogging.LogAction("Get workflow job error", "Parsed Error: " + returnError);

        } catch (IOException e) {
            UserActionLogging.LogAction("Get workflow job error", "Exception: " + e.getMessage());
            return null;
        }

        UserActionLogging.LogAction("Get workflow job error", "Completed successfully!");

        return returnError;
    }


}
