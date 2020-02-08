package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputDefinition;
import com.google.common.collect.Lists;
import com.google.gson.*;
import eu.hbp.mip.controllers.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.retrofit.RetrofitClientInstance;
import eu.hbp.mip.dto.ErrorResponse;
import eu.hbp.mip.dto.GalaxyWorkflowResult;
import eu.hbp.mip.dto.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.model.*;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.repositories.ModelRepository;
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
import org.springframework.web.bind.annotation.*;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/experiments")
public class ExperimentApi {

    //private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentApi.class);

    private static final Gson gson = new Gson();

    private static final Gson gsonOnlyExposed = new GsonBuilder().serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").excludeFieldsWithoutExposeAnnotation().create();

    @Value("#{'${services.exareme.miningExaremeUrl:http://localhost:9090/mining/query}'}")
    public String miningExaremeQueryUrl;

    @Value("#{'${services.workflows.workflowUrl}'}")
    private String workflowUrl;

    @Value("#{'${services.workflows.jwtSecret}'}")
    private String jwtSecret;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @ApiOperation(value = "Create an experiment on Exareme", response = Experiment.class)
    @RequestMapping(value = "/exareme", method = RequestMethod.POST)
    public ResponseEntity<String> runExaremeExperiment(@RequestBody ExperimentQuery expQuery) {
        //LOGGER.info("send ExaremeExperiment");

        Experiment experiment = saveExperiment(expQuery);

        String algoCode = expQuery.getAlgorithms().get(0).getCode();
        List<AlgorithmParam> params = expQuery.getAlgorithms().get(0).getParameters();
        new Thread(() -> {
            List<HashMap<String, String>> queryList = new ArrayList<HashMap<String, String>>();

            if (params != null) {
                for (AlgorithmParam p : params) {
                    queryList.add(makeObject(p.getName(), p.getValue()));
                }
            }

            String query = gson.toJson(queryList);
            String url = miningExaremeQueryUrl + "/" + algoCode;

            // Results are stored in the experiment object
            try {
                StringBuilder results = new StringBuilder();
                int code = HTTPUtil.sendPost(url, query, results);
                experiment.setResult("[" + results.toString() + "]");
                experiment.setHasError(code >= 400);
                experiment.setHasServerError(code >= 500);
            } catch (IOException e) {
                //LOGGER.trace("Invalid UUID", e);
                //LOGGER.warn("Exareme experiment failed to run properly !");
                experiment.setHasError(true);
                experiment.setHasServerError(true);
                experiment.setResult(e.getMessage());
            }
            finishExperiment(experiment);
        }).start();

        UserActionLogging.LogAction("create ExaremeExperiment", "no info");

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "Create a workflow", response = Experiment.class)
    @RequestMapping(value = "/workflow", method = RequestMethod.POST)
    public ResponseEntity runWorkflow(@RequestBody ExperimentQuery expQuery) {
        UserActionLogging.LogAction("Run workflow", "Running a workflow...");

        Experiment experiment = saveExperiment(expQuery);

        // Get the algorithm parameters provided
        String algorithmName = expQuery.getAlgorithms().get(0).getName();
        List<AlgorithmParam> algorithmParams = expQuery.getAlgorithms().get(0).getParameters();
        HashMap<String, String> algorithmParamsIncludingEmpty = new HashMap<>();
        if (algorithmParams != null) {
            for (AlgorithmParam p : algorithmParams) {
                algorithmParamsIncludingEmpty.put(p.getName(), p.getValue());
            }
        }

        // Get all the algorithm parameters because the frontend provides only the non-null
        final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
        final WorkflowsClient workflowsClient = instance.getWorkflowsClient();
        Workflow workflow = null;
        for (Workflow curWorkflow : workflowsClient.getWorkflows()) {
            if (curWorkflow.getName().equals(algorithmName)) {
                workflow = curWorkflow;
                break;
            }
        }
        if (workflow == null) {
            UserActionLogging.LogAction("Run workflow", "Could not find algorithm code");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Could not find galaxy algorithm.", "99"));
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
        UserActionLogging.LogAction("Run workflow", "Running Galaxy workflow with id: " + workflow.getId());

        // Call Galaxy to run the workflow
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(workflow.getId(), galaxyApiKey, requestBodyJson);
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();

            if (response.code() == 200) {       // Call succeeded
                String responseBody = gson.toJson(response.body());
                UserActionLogging.LogAction("Run workflow", "Response: " + responseBody);

                String historyId = (String) new JSONObject(responseBody).get("history_id");
                experiment.setWorkflowHistoryId(historyId);
                experiment.setWorkflowStatus("running");
                experiment.setHasError(false);
                experiment.setHasServerError(response.code() >= 500);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                UserActionLogging.LogAction("Run workflow", "Error Response: " + msgErr);

                // Values are read from streams.
                JSONObject jObjectError = new JSONObject(msgErr);
                String errMsg = jObjectError.get("err_msg").toString();
                String errCode = jObjectError.get("err_code").toString();

                experiment.setResult(new ErrorResponse(errMsg, errCode).toString());
                experiment.setHasError(response.code() >= 400);
                experiment.setHasServerError(response.code() >= 500);
            }

        } catch (Exception e) {
            UserActionLogging.LogAction("Run workflow", "An exception occurred: " + e.getMessage());
            experiment.setHasError(true);
            experiment.setHasServerError(true);
            experiment.setResult(e.getMessage());
        }
        finishExperiment(experiment);
        UserActionLogging.LogAction("Run workflow", "Run workflow completed!");

        return new ResponseEntity(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "get an experiment", response = Experiment.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public ResponseEntity<String> getExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        Experiment experiment;
        UUID experimentUuid;
        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            UserActionLogging.LogAction("Get Experiment", "Invalid Experiment UUID.");
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);

        if (experiment == null) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        UserActionLogging.LogAction("Get an experiment ", " uuid : " + uuid);

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "get a workflow", response = Experiment.class)
    @RequestMapping(value = "/workflow/{experimentId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowExperiment(
            @ApiParam(value = "experimentId", required = true) @PathVariable("experimentId") String experimentId) {
        UserActionLogging.LogAction("Get workflow experiment", " Experiment Id : " + experimentId);

        Experiment experiment;
        UUID experimentUuid;
        try {
            experimentUuid = UUID.fromString(experimentId);
        } catch (IllegalArgumentException iae) {
            UserActionLogging.LogAction("Get workflow experiment", " Invalid Experiment Id");
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);
        if (experiment == null) {
            UserActionLogging.LogAction("Get workflow experiment", "The experiment does not exist.");
            return ResponseEntity.badRequest().body("The experiment does not exist.");
        }

        if (experiment.getWorkflowHistoryId() == null) {
            UserActionLogging.LogAction("Get workflow experiment", "History Id does not exist.");
            return ResponseEntity.badRequest().body("The experiment is not a workflow.");
        }

        // If result already exists return
        if (experiment.getResult() != null) {
            UserActionLogging.LogAction("Get workflow experiment",
                    "Result exists: " + experiment.getResult());
            return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
        }

        // If result doesn't exist, fetch it
        UserActionLogging.LogAction("Get workflow experiment", "Result is null.");

        String state = getWorkflowStatus(experiment.getWorkflowHistoryId());
        UserActionLogging.LogAction("Get workflow experiment", "State is: " + state);

        switch (state) {
            case "running":
                // Do nothing, when the experiment is created the status is set to running
                UserActionLogging.LogAction("Get workflow experiment", "Result is still running.");
                break;

            case "completed":
                // Get only the job result that is visible
                List<GalaxyWorkflowResult> workflowJobsResults = getWorkflowResults(experiment.getWorkflowHistoryId());
                UserActionLogging.LogAction("Get workflow experiment",
                        "Results are: " + workflowJobsResults.toString());

                boolean resultFound = false;
                for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                    if (jobResult.getVisible()) {
                        UserActionLogging.LogAction("Get workflow experiment",
                                "Visible result are: " + jobResult.getId());

                        String result = getWorkflowResultBody(experiment.getWorkflowHistoryId(), jobResult.getId());

                        UserActionLogging.LogAction("Get workflow experiment", "Result: " + result);
                        if (result == null) {
                            experiment.setHasError(true);
                            experiment.setHasServerError(true);
                        }
                        experiment.setResult(result);
                        experiment.setWorkflowStatus("completed");
                        resultFound = true;
                    }
                }

                if (!resultFound) {      // If there is no visible result
                    UserActionLogging.LogAction("Get workflow experiment", "No visible result");
                    experiment.setResult(new ErrorResponse("The workflow has no visible result.", "500").toString());
                    experiment.setHasError(true);
                    experiment.setHasServerError(true);
                }
                break;

            case "error":
                // Get the job result that failed
                workflowJobsResults = getWorkflowResults(experiment.getWorkflowHistoryId());
                UserActionLogging.LogAction("Get workflow experiment",
                        "Error results are: " + workflowJobsResults.toString());

                boolean failedJobFound = false;
                for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                    if (jobResult.getState().equals("error")) {
                        UserActionLogging.LogAction("Get workflow experiment",
                                "Failed job is: " + jobResult.getId());

                        String result = getWorkflowJobError(jobResult.getId());

                        UserActionLogging.LogAction("Get workflow experiment", "Job result: " + result);
                        if (result == null) {
                            experiment.setHasError(true);
                            experiment.setHasServerError(true);
                        }
                        experiment.setResult(result);
                        experiment.setWorkflowStatus("error");
                        failedJobFound = true;
                    }
                }

                if (!failedJobFound) {      // If there is no visible failed job
                    UserActionLogging.LogAction("Get workflow experiment", "No failed result");
                    experiment.setResult(new ErrorResponse("The workflow has no failed result.", "500").toString());
                    experiment.setHasError(true);
                    experiment.setHasServerError(true);
                }
                break;

            default:        // InternalError or unexpected result
                experiment.setResult(new ErrorResponse("An unexpected error occurred.", "500").toString());
                experiment.setHasError(true);
                experiment.setHasServerError(true);
                break;
        }

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }


    /**
     * @param historyId The historyId of the workflow
     * @return "running"           ->      When the workflow is still running
     * "internalError"     ->      When an exception or a bad request occurred
     * "error"             ->      When the workflow produced an error
     * "completed"         ->      When the workflow completed successfully
     */
    public String getWorkflowStatus(String historyId) {
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

    @ApiOperation(value = "Mark an experiment as viewed", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsViewed", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsViewed(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        UserActionLogging.LogAction("Mark an experiment as viewed", " uuid : " + uuid);

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
        experiment.setResultsViewed(true);
        experimentRepository.save(experiment);

        UserActionLogging.LogAction("Experiment updated (marked as viewed)", " ");

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "Mark an experiment as shared", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsShared", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsShared(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {

        UserActionLogging.LogAction("Mark an experiment as shared", " uuid : " + uuid);

        return doMarkExperimentAsShared(uuid, true);
    }

    @ApiOperation(value = "Mark an experiment as unshared", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsUnshared", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsUnshared(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        UserActionLogging.LogAction("Mark an experiment as unshared", " uuid : " + uuid);

        return doMarkExperimentAsShared(uuid, false);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"maxResultCount"})
    public ResponseEntity<String> listExperiments(
            @ApiParam(value = "maxResultCount") @RequestParam int maxResultCount) {

        UserActionLogging.LogAction("List experiments", " maxResultCount : " + maxResultCount);

        return doListExperiments(false, null);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"slug", "maxResultCount"})
    public ResponseEntity<String> listExperiments(@ApiParam(value = "slug") @RequestParam("slug") String modelSlug,
                                                  @ApiParam(value = "maxResultCount") @RequestParam("maxResultCount") int maxResultCount) {

        UserActionLogging.LogAction("List experiments", " modelSlug : " + modelSlug);

        if (maxResultCount <= 0 && (modelSlug == null || "".equals(modelSlug))) {
            return new ResponseEntity<>("You must provide at least a slug or a limit of result",
                    HttpStatus.BAD_REQUEST);
        }

        return doListExperiments(false, modelSlug);
    }

    @ApiOperation(value = "list my experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = {"mine"})
    public ResponseEntity<String> listMyExperiments(@ApiParam(value = "mine") @RequestParam("mine") boolean mine) {
        UserActionLogging.LogAction("List my experiments", " mine : " + mine);

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

        UserActionLogging.LogAction("Experiment updated (marked as shared)", "");

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    private void finishExperiment(Experiment experiment) {
        experiment.setFinished(new Date());
        experimentRepository.save(experiment);

        UserActionLogging.LogAction("Experiment updated (finished)", "");
    }

    private HashMap<String, String> makeObject(String name, String value) {
        HashMap<String, String> o = new HashMap<String, String>();
        o.put("name", name);
        o.put("value", value);

        return o;
    }

    private Experiment saveExperiment(ExperimentQuery expQuery) {

        Experiment experiment = new Experiment();
        experiment.setUuid(UUID.randomUUID());
        User user = userInfo.getUser();

        experiment.setAlgorithms(gson.toJson(expQuery.getAlgorithms()));
        experiment.setValidations(gson.toJson(expQuery.getValidations()));
        experiment.setName(expQuery.getName());
        experiment.setCreatedBy(user);
        experiment.setModel(modelRepository.findOne(expQuery.getModel()));
        experimentRepository.save(experiment);

        UserActionLogging.LogAction("Saved an experiment", " id : " + experiment.getUuid());

        return experiment;
    }

}
