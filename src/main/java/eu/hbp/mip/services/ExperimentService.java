package eu.hbp.mip.services;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputDefinition;
import com.google.gson.*;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.model.DAOs.ExperimentDAO;
import eu.hbp.mip.model.DAOs.UserDAO;
import eu.hbp.mip.model.DTOs.AlgorithmDTO;
import eu.hbp.mip.model.DTOs.ExperimentDTO;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.model.galaxy.ErrorResponse;
import eu.hbp.mip.model.galaxy.GalaxyWorkflowResult;
import eu.hbp.mip.model.galaxy.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.utils.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;

@Service
public class ExperimentService {

    @Autowired
    private UserInfo userInfo;

    @Value("#{'${services.exareme.queryExaremeUrl}'}")
    private String queryExaremeUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${hbp.authentication.enabled:1}'}")
    private boolean authenticationIsEnabled;

    @Autowired
    private ExperimentRepository experimentRepository;

    private static final Gson gson = new Gson();

    public ResponseEntity<String> getExperiments(String name,String algorithm, Boolean shared,Boolean viewed, int page, int size, String endpoint) {
        UserDAO user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Listing my experiments.");
        if(size > 10 )
            return ResponseEntity.badRequest().body("Invalid size input, max size is 10.");

        Specification<ExperimentDAO> spec = Specification.where(new ExperimentSpecifications.ExperimentWithName(name))
                .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                .and(new ExperimentSpecifications.ExperimentWithViewed(viewed));

        Pageable paging = PageRequest.of(page, size);
        Page<ExperimentDAO> pageExperiments = experimentRepository.findAll(spec, paging);
        List<ExperimentDAO> experimentDAOs = pageExperiments.getContent();

        if (experimentDAOs.isEmpty())
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        List<ExperimentDTO> experimentDTOs = new ArrayList<>();
        experimentDAOs.forEach(experimentDAO -> experimentDTOs.add(experimentDAO.convertToDTO()));

        Map<String, Object> response = new HashMap<>();
        response.put("experiments", experimentDTOs);
        response.put("currentPage", pageExperiments.getNumber());
        response.put("totalExperiments", pageExperiments.getTotalElements());
        response.put("totalPages", pageExperiments.getTotalPages());

        return new ResponseEntity(response, HttpStatus.OK);
    }

    public ResponseEntity<String> getExperiment(String uuid, String endpoint) {

        ExperimentDAO experimentDAO;
        UserDAO user = userInfo.getUser();

        Logging.LogUserAction(user.getUsername(), endpoint, "Loading Experiment with uuid : " + uuid);

        experimentDAO = loadExperiment(uuid).orElseThrow(() -> new ResourceNotFoundException("Not found Experimnet with id = " + uuid));

        if (experimentDAO == null) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Experiment Not found.");
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        if (!experimentDAO.isShared() && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername())) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Accessing Experiment is unauthorized.");
            return new ResponseEntity<>("You don't have access to the experiment.", HttpStatus.UNAUTHORIZED);
        }
        ExperimentDTO experimentDTO = experimentDAO.convertToDTO();
        Logging.LogUserAction(user.getUsername(), endpoint, "Experiment was Loaded with uuid : " + uuid + ".");

        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }

    public ResponseEntity<String> createExperiment(Authentication authentication, ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = userInfo.getUser();

        //Checking if check (POST) /experiments has proper input.
        if (checkPostExperimentProperInput(experimentDTO)){
            Logging.LogUserAction(user.getUsername(), endpoint,
                    "Invalid input.");
            return ResponseEntity.badRequest().body("Please provide proper input.");
        }
        // Get the type and name of algorithm
        String algorithmType = experimentDTO.getAlgorithmDetails().getType();
        String algorithmName = experimentDTO.getAlgorithmDetails().getName();

        StringBuilder parametersLogMessage = new StringBuilder(", Parameters:\n");
        for (AlgorithmDTO.AlgorithmParamDTO params : experimentDTO.getAlgorithmDetails().getParameters()) {
            parametersLogMessage
                    .append("  ")
                    .append(params.getLabel())
                    .append(" -> ")
                    .append(params.getValue())
                    .append("\n");
        }
        Logging.LogUserAction(user.getUsername(), endpoint, "Executing " + algorithmName + parametersLogMessage);

        if (authenticationIsEnabled) {
            // Getting the dataset from the experiment parameters
            String experimentDatasets = null;
            for (AlgorithmDTO.AlgorithmParamDTO parameter : experimentDTO.getAlgorithmDetails().getParameters()) {
                if (parameter.getLabel().equals("dataset")) {
                    experimentDatasets = parameter.getValue();
                    break;
                }
            }

            if (experimentDatasets == null || experimentDatasets.equals("")) {
                Logging.LogUserAction(user.getUsername(), endpoint,
                        "A dataset should be specified to run an algorithm.");
                return ResponseEntity.badRequest().body("Please provide at least one dataset to run the algorithm.");
            }

            // --- Validating proper access rights on the datasets  ---
            if (!ClaimUtils.userHasDatasetsAuthorization(user.getUsername(), authentication.getAuthorities(), experimentDatasets)) {
                return ResponseEntity.badRequest().body("You are not authorized to use these datasets.");
            }
        }

        // Run with the appropriate engine
        if (algorithmType.equals("workflow")) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Algorithm runs on Galaxy.");
            return runGalaxyWorkflow(experimentDTO, endpoint);
        } else {
            Logging.LogUserAction(user.getUsername(), endpoint, "Algorithm runs on Exareme.");
            return createExaremeExperiment(experimentDTO, endpoint);
        }
    }

    public ResponseEntity<String> updateExperiment(String uuid, ExperimentDTO experimentDTO, String endpoint)
    {
        ExperimentDAO experimentDAO;
        UserDAO user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Updating experiment with uuid : " + experimentDTO.getUuid() + ".");
        //Checking if check (PUT) /experiments has proper input.
        if (checkPutExperimentProperInput(experimentDTO)){
            Logging.LogUserAction(user.getUsername(), endpoint,
                    "Invalid input.");
            return ResponseEntity.badRequest().body("Please provide proper input.");
        }

        if((experimentDTO.getName() == null || experimentDTO.getName().length() == 0)
                && experimentDTO.getShared() == null
                && experimentDTO.getViewed() == null
                && experimentDTO.getAlgorithmDetails() == null)
        {
            return ResponseEntity.badRequest().body("Input is required.");
        }

        experimentDAO = loadExperiment(uuid).get();

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()))
            return new ResponseEntity<>("You're not the owner of this experiment", HttpStatus.UNAUTHORIZED);

        if(experimentDTO.getName() != null && experimentDTO.getName().length() != 0)
        {
            experimentDAO.setName(experimentDTO.getName());
        }

        if(experimentDTO.getShared() != null)
        {
            experimentDAO.setShared(experimentDTO.getShared());
        }

        if(experimentDTO.getViewed() != null)
        {
            experimentDAO.setViewed(experimentDTO.getViewed());
        }

        experimentRepository.save(experimentDAO);

        Logging.LogUserAction(user.getUsername(), endpoint, "Updated experiment with uuid : " + experimentDTO.getUuid() + ".");

        experimentDTO = experimentDAO.convertToDTO();
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }

    public ResponseEntity<String> deleteExperiment(String uuid, String endpoint)
    {
        ExperimentDAO experimentDAO;
        UserDAO user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Deleting experiment with uuid : " + uuid + ".");

        experimentDAO = loadExperiment(uuid).get();

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()))
            return new ResponseEntity<>(endpoint, HttpStatus.UNAUTHORIZED);

        experimentRepository.delete(experimentDAO);

        Logging.LogUserAction(user.getUsername(), endpoint, "Deleted experiment with uuid : " + uuid + ".");
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    //    /* -------------------------------  PRIVATE METHODS  ----------------------------------------------------*/
    private boolean checkPostExperimentProperInput(ExperimentDTO experimentDTO)
    {
        return  experimentDTO.getShared() != null
                || experimentDTO.getViewed() != null
                || experimentDTO.getCreated() != null
                || experimentDTO.getCreatedBy() != null
                || experimentDTO.getResult() != null
                || experimentDTO.getStatus() != null
                || experimentDTO.getUuid() != null;
    }

    private boolean checkPutExperimentProperInput(ExperimentDTO experimentDTO)
    {
        return experimentDTO.getUuid() != null
                || experimentDTO.getCreated() != null
                || experimentDTO.getResult() != null
                || experimentDTO.getStatus() != null;
    }

    private Optional<ExperimentDAO> loadExperiment(String uuid){


        UUID experimentUuid ;

        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            return null;
        }

        return experimentRepository.findByUuid(experimentUuid);
    }

    private ExperimentDAO createExperiment(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = userInfo.getUser();

        ExperimentDAO experimentDAO = new ExperimentDAO();
        experimentDAO.setUuid(UUID.randomUUID());
        experimentDAO.setCreatedBy(user);
        experimentDAO.setAlgorithmDetails(JsonConverters.convertObjectToJsonString(experimentDTO.getAlgorithmDetails()));
        experimentDAO.setAlgorithm(experimentDTO.getAlgorithm());
        experimentDAO.setName(experimentDTO.getName());
        experimentRepository.save(experimentDAO);

        Logging.LogUserAction(user.getUsername(), endpoint, " id : " + experimentDAO.getUuid());
        Logging.LogUserAction(user.getUsername(), endpoint, " algorithms : " + experimentDAO.getAlgorithmDetails());
        Logging.LogUserAction(user.getUsername(), endpoint, " name : " + experimentDAO.getName());
        return experimentDAO;
    }

    private void saveExperiment(ExperimentDAO experimentDAO, String endpoint) {
        UserDAO user = userInfo.getUser();

        Logging.LogUserAction(user.getUsername(), endpoint, " id : " + experimentDAO.getUuid());
        Logging.LogUserAction(user.getUsername(), endpoint, " algorithms : " + experimentDAO.getAlgorithmDetails());
        Logging.LogUserAction(user.getUsername(), endpoint, " name : " + experimentDAO.getName());
        Logging.LogUserAction(user.getUsername(), endpoint, " historyId : " + experimentDAO.getWorkflowHistoryId());
        Logging.LogUserAction(user.getUsername(), endpoint, " status : " + experimentDAO.getStatus());

        experimentRepository.save(experimentDAO);

        Logging.LogUserAction(user.getUsername(), endpoint, "Saved experiment");
    }

    private void finishExperiment(ExperimentDAO experimentDAO) {
        experimentDAO.setFinished(new Date());
        experimentRepository.save(experimentDAO);
    }

    /* --------------------------------------  EXAREME CALLS ---------------------------------------------------------*/

    /**
     * The runExaremeExperiment will POST the algorithm to the exareme client
     *
     * @param experimentDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ResponseEntity<String> createExaremeExperiment(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Running the algorithm...");

        ExperimentDAO experimentDAO = createExperiment(experimentDTO, endpoint);
        Logging.LogUserAction(user.getUsername(), endpoint, "Created experiment with uuid :" + experimentDAO.getUuid());

        // Run the 1st algorithm from the list
        String algorithmName = experimentDTO.getAlgorithmDetails().getName();

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithmDetails().getParameters();

        String body = gson.toJson(algorithmParameters);
        String url = queryExaremeUrl + "/" + algorithmName;
        Logging.LogUserAction(user.getUsername(), endpoint, "url: " + url + ", body: " + body);

        Logging.LogUserAction(user.getUsername(), endpoint,
                "Completed, returning: " + experimentDTO.toString());

        Logging.LogUserAction(user.getUsername(), endpoint,
                "Starting exareme execution thread");
        new Thread(() -> {

            // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogUserAction!
            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread named :" + Thread.currentThread().getName() + " with id :" + Thread.currentThread().getId() + " started!");

            try {

                // --> New function runExaremeAlgorithm
                StringBuilder results = new StringBuilder();
                int code = HTTPUtil.sendPost(url, body, results);

                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Algorithm finished with code: " + code);

                // Results are stored in the experiment object
                ExperimentDTO.ResultDTO resultDTO = JsonConverters.convertJsonStringToObject(String.valueOf(results), ExperimentDTO.ResultDTO.class);

                // <-- New function runExaremeAlgorithm

                experimentDAO.setResult(JsonConverters.convertObjectToJsonString(resultDTO));
//                experimentDAO.setHasError(code >= 400);
//                experimentDAO.setHasServerError(code >= 500);
            } catch (Exception e) {
                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "There was an exception: " + e.getMessage());

                experimentDAO.setStatus(ExperimentDAO.Status.error);
                experimentDAO.setResult(e.getMessage());
            }

            finishExperiment(experimentDAO);
            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Finished the experiment: " + experimentDAO.toString());
        }).start();
        experimentDTO = experimentDAO.convertToDTO();
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.CREATED);
    }

    /* ---------------------------------------  GALAXY CALLS ---------------------------------------------------------*/


    /**
     * The runWorkflow will POST the algorithm to the galaxy client
     *
     * @param experimentDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ResponseEntity<String> runGalaxyWorkflow(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Running a workflow...");

        ExperimentDAO experimentDAO = createExperiment(experimentDTO, endpoint);
        Logging.LogUserAction(user.getUsername(), endpoint, "Created experiment with uuid :" + experimentDAO.getUuid());


        // Run the 1st algorithm from the list
        String workflowId = experimentDTO.getAlgorithmDetails().getName();

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithmDetails().getParameters();

        // Convert the parameters to workflow parameters
        HashMap<String, String> algorithmParamsIncludingEmpty = new HashMap<>();
        if (algorithmParameters != null) {
            for (AlgorithmDTO.AlgorithmParamDTO param : algorithmParameters) {
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
            Logging.LogUserAction(user.getUsername(), endpoint,
                    "Could not find algorithm code: " + workflowId);
            return ResponseEntity.badRequest()
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
        Logging.LogUserAction(user.getUsername(), endpoint, "Running Galaxy workflow with id: " + workflow.getId());

        // Call Galaxy to run the workflow
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(workflow.getId(), galaxyApiKey, requestBodyJson);
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();

            if (response.code() == 200) {       // Call succeeded
                String responseBody = gson.toJson(response.body());
                Logging.LogUserAction(user.getUsername(), endpoint, "Response: " + responseBody);

                String historyId = (String) new JSONObject(responseBody).get("history_id");
                experimentDAO.setWorkflowHistoryId(historyId);
                experimentDAO.setStatus(ExperimentDAO.Status.pending);
//                experimentDAO.setHasServerError(response.code() >= 500);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                Logging.LogUserAction(user.getUsername(), endpoint, "Error Response: " + msgErr);

                // Values are read from streams.
                JSONObject jObjectError = new JSONObject(msgErr);
                String errMsg = jObjectError.get("err_msg").toString();

                experimentDAO.setResult(errMsg);
//                experimentDAO.setHasError(response.code() >= 400);
//                experimentDAO.setHasServerError(response.code() >= 500);
            }

        } catch (Exception e) {
            Logging.LogUserAction(user.getUsername(), endpoint, "An exception occurred: " + e.getMessage());
            experimentDAO.setStatus(ExperimentDAO.Status.error);
            experimentDAO.setResult(e.getMessage());
        }
        saveExperiment(experimentDAO, endpoint);

        // Start the process of fetching the status
        updateWorkflowExperiment(experimentDAO, endpoint);

        Logging.LogUserAction(user.getUsername(), endpoint, "Run workflow completed!");

        experimentDTO = experimentDAO.convertToDTO();
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.CREATED);
    }


    /**
     * This method creates a thread that will fetch the workflow result when it is ready
     *
     * @param experimentDAO will be used to fetch it's workflow status, it should have the workflowHistoryId initialized
     *                   and the result should not already be fetched
     * @return nothing, just updates the experiment
     */
    public void updateWorkflowExperiment(ExperimentDAO experimentDAO, String endpoint) {
        UserDAO user = userInfo.getUser();

        if (experimentDAO == null) {
            Logging.LogUserAction(user.getUsername(), endpoint, "The experiment does not exist.");
            return;
        }

        Logging.LogUserAction(user.getUsername(), endpoint,
                " Experiment id : " + experimentDAO.getUuid());
        if (experimentDAO.getWorkflowHistoryId() == null) {
            Logging.LogUserAction(user.getUsername(), endpoint, "History Id does not exist.");
            return;
        }

        Logging.LogUserAction(user.getUsername(), endpoint, "Starting Thread...");
        new Thread(() -> {
            while (true) {
                // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogExperimentAction!
                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread is running...");

                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Sleep was disrupted: " + e.getMessage());
                }

                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Fetching status for experiment Id: " + experimentDAO.getUuid());

                String state = getWorkflowStatus(experimentDAO);
                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "State is: " + state);

                switch (state) {
                    case "running":
                        // Do nothing, when the experiment is created the status is set to running
                        Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Workflow is still running.");
                        break;

                    case "completed":
                        // Get only the job result that is visible
                        List<GalaxyWorkflowResult> workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Results are: " + workflowJobsResults.toString());

                        boolean resultFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getVisible()) {
                                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Visible result are: " + jobResult.getId());

                                String result = getWorkflowResultBody(experimentDAO, jobResult.getId());

                                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                }
                                experimentDAO.setResult("[" + result + "]");
                                experimentDAO.setStatus(ExperimentDAO.Status.success);
                                resultFound = true;
                            }
                        }

                        if (!resultFound) {      // If there is no visible result
                            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No visible result");
                            experimentDAO.setResult("[" + new ErrorResponse("The workflow has no visible result.").toString() + "]");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }

                        finishExperiment(experimentDAO);
                        break;

                    case "error":
                        // Get the job result that failed
                        workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Error results are: " + workflowJobsResults.toString());

                        boolean failedJobFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getState().equals("error")) {
                                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Failed job is: " + jobResult.getId());

                                String result = getWorkflowJobError(jobResult.getId(), experimentDAO);

                                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Job result: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                }
                                experimentDAO.setResult("[" + result + "]");
                                experimentDAO.setStatus(ExperimentDAO.Status.error);
                                failedJobFound = true;
                            }
                        }

                        if (!failedJobFound) {      // If there is no visible failed job
                            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No failed result");
                            experimentDAO.setResult("[" + new ErrorResponse("The workflow has no failed result.").toString() + "]");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }
                        finishExperiment(experimentDAO);
                        break;

                    default:        // InternalError or unexpected result
                        experimentDAO.setResult("[" + new ErrorResponse("An unexpected error occurred.").toString() + "]");
                        experimentDAO.setStatus(ExperimentDAO.Status.error);
                        finishExperiment(experimentDAO);
                        break;
                }

                // If result exists return
                if (experimentDAO.getResult() != null) {
                    Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO exists: " + experimentDAO.getResult());
                    return;
                }
            }
        }).start();
    }


    /**
     * @param experimentDAO The experiment of the workflow
     * @return "running"           ->      When the workflow is still running
     * "internalError"     ->      When an exception or a bad request occurred
     * "error"             ->      When the workflow produced an error
     * "completed"         ->      When the workflow completed successfully
     */
    public String getWorkflowStatus(ExperimentDAO experimentDAO) {
        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        // ATTENTION: This function is used from a Thread. Only LogExperimentAction should be used, not LogUserAction!
        Logging.LogExperimentAction(experimentName, experimentId, " History Id : " + historyId);

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(historyId, galaxyApiKey);

        String result = null;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logging.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return "internalError";
            }
            result = new Gson().toJson(response.body());
            Logging.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + result);

        } catch (IOException e) {
            Logging.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        String state = null;
        try {
            JSONObject resultJson = new JSONObject(result);
            state = resultJson.getString("state");
        } catch (JSONException e) {
            Logging.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        Logging.LogExperimentAction(experimentName, experimentId, " Completed!");
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
     * @param experimentDAO The experiment of the workflow
     * @return a List<GalaxyWorkflowResult>   or null when an error occurred
     */
    public List<GalaxyWorkflowResult> getWorkflowResults(ExperimentDAO experimentDAO) {

        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();
        Logging.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<List<GalaxyWorkflowResult>> call = service.getWorkflowResultsFromGalaxy(historyId, galaxyApiKey);

        List<GalaxyWorkflowResult> getGalaxyWorkflowResultList = null;
        try {
            Response<List<GalaxyWorkflowResult>> response = call.execute();
            if (response.code() >= 400) {
                Logging.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            getGalaxyWorkflowResultList = response.body();
            Logging.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + response.body());

        } catch (IOException e) {
            Logging.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return null;
        }

        Logging.LogExperimentAction(experimentName, experimentId, " Completed!");
        return getGalaxyWorkflowResultList;

    }

    /**
     * @param experimentDAO The experiment of the workflow
     * @param contentId  the id of the job result that we want
     * @return the result of the specific workflow job, null if there was an error
     */
    public String getWorkflowResultBody(ExperimentDAO experimentDAO, String contentId) {

        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logging.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call =
                service.getWorkflowResultsBodyFromGalaxy(historyId, contentId, galaxyApiKey);

        String resultJson = null;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logging.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            resultJson = new Gson().toJson(response.body());
            Logging.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + resultJson);

        } catch (IOException e) {
            Logging.LogExperimentAction(experimentName, experimentId,
                    " An exception happened: " + e.getMessage());
            return null;
        }

        Logging.LogExperimentAction(experimentName, experimentId, " Completed!");
        return resultJson;
    }


    /**
     * @param jobId the id of the workflow job that failed
     * @return the error that was produced or null if an error occurred
     */
    public String getWorkflowJobError(String jobId, ExperimentDAO experimentDAO) {
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logging.LogExperimentAction(experimentName, experimentId, " jobId : " + jobId);
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> callError = service.getErrorMessageOfWorkflowFromGalaxy(jobId, galaxyApiKey);

        String fullError = null;
        String returnError = null;
        try {
            Response<Object> response = callError.execute();
            if (response.code() >= 400) {
                Logging.LogExperimentAction(experimentName, experimentId, "Response code: "
                        + response.code() + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }

            // Parsing the stderr of the job that failed
            String jsonString = new Gson().toJson(response.body());
            JsonElement jsonElement = new JsonParser().parse(jsonString);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            fullError = rootObject.get("stderr").getAsString();
            Logging.LogExperimentAction(experimentName, experimentId, "Error: " + fullError);

            String[] arrOfStr = fullError.split("ValueError", 0);
            String specError = arrOfStr[arrOfStr.length - 1];
            returnError = specError.substring(1);
            Logging.LogExperimentAction(experimentName, experimentId, "Parsed Error: " + returnError);

        } catch (IOException e) {
            Logging.LogExperimentAction(experimentName, experimentId, "Exception: " + e.getMessage());
            return null;
        }

        Logging.LogExperimentAction(experimentName, experimentId, "Completed successfully!");

        return returnError;
    }
}
