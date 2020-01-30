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
import eu.hbp.mip.dto.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.helpers.LogHelper;
import eu.hbp.mip.model.*;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.repositories.ModelRepository;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.JWTUtil;
import eu.hbp.mip.utils.UserActionLogging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

/**
 * Created by habfast on 21/04/16.
 */
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
                experiment.setResult("");
                experiment.setHasError(false);
                experiment.setHasServerError(response.code() >= 500);

            }else {     // Something unexpected happened
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
            //LOGGER.trace("Invalid UUID", iae);
            //LOGGER.warn("An invalid Experiment UUID was received ! " + uuid);
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);

        if (experiment == null) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        UserActionLogging.LogAction("Get an experiment ", " uuid : " + uuid);

        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "get workflow status", response = String.class)
    @RequestMapping(value = "/workflow/status/{experimentId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowStatus(
            @ApiParam(value = "historyId", required = true) @PathVariable("experimentId") String experimentId) {

        UserActionLogging.LogAction("Get a workflow status", " Experiment Id : " + experimentId);

        Experiment experiment;
        UUID experimentUuid;
        try {
            experimentUuid = UUID.fromString(experimentId);
        } catch (IllegalArgumentException iae) {
            UserActionLogging.LogAction("Get a workflow status", " Invalid Experiment Id");
            return ResponseEntity.badRequest().body("Invalid Experiment UUID");
        }

        experiment = experimentRepository.findOne(experimentUuid);
        if(experiment == null){
            UserActionLogging.LogAction("Get a workflow status", "The experiment does not exist.");
            return ResponseEntity.badRequest().body("The experiment does not exist.");
        }

        if(experiment.getWorkflowHistoryId() == null){
            UserActionLogging.LogAction("Get a workflow status", "History Id does not exist.");
            return ResponseEntity.badRequest().body("The experiment is not a workflow.");
        }

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(experiment.getWorkflowHistoryId(), galaxyApiKey);

        String resultJson = null;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                UserActionLogging.LogAction("Get a workflow status", " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return ResponseEntity.status(response.code()).body((response.errorBody() != null ? response.errorBody().string() : " "));
            }
            resultJson = new Gson().toJson(response.body());
            UserActionLogging.LogAction("Get a workflow status", " Result: " + resultJson);

        } catch (IOException e) {
            UserActionLogging.LogAction("Get a workflow status"
                    , " An exception happend: " + e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }

        UserActionLogging.LogAction("Get a workflow status", " Completed!");
        return ResponseEntity.ok(resultJson);
    }

    // TODO: factorize workflow results
    @ApiOperation(value = "get workflow results", response = String.class)
    @RequestMapping(value = "/workflow/results/{historyId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowResults(
            @ApiParam(value = "historyId", required = true) @PathVariable("historyId") String historyId) {
        UserActionLogging.LogAction("Get workflow results", " historyId : " + historyId);

        String url = workflowUrl + "/getWorkflowResults/" + historyId;
        try {
            StringBuilder response = new StringBuilder();
            User user = userInfo.getUser();
            String token = JWTUtil.getJWT(jwtSecret, user.getEmail());
            HTTPUtil.sendAuthorizedHTTP(url, "", response, "GET", "Bearer " + token);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @ApiOperation(value = "get workflow result body", response = String.class)
    @RequestMapping(value = "/workflow/resultsbody/{historyId}/content/{resultId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowResultBody(
            @ApiParam(value = "historyId", required = true) @PathVariable("historyId") String historyId,
            @ApiParam(value = "resultId", required = true) @PathVariable("resultId") String resultId) {

        UserActionLogging.LogAction("Get workflow result content", " historyId : " + historyId + " resultId : " + resultId);

        String url = workflowUrl + "/getWorkflowResultsBody/" + historyId + "/contents/" + resultId;
        try {
            StringBuilder response = new StringBuilder();
            User user = userInfo.getUser();
            String token = JWTUtil.getJWT(jwtSecret, user.getEmail());
            HTTPUtil.sendAuthorizedHTTP(url, "", response, "GET", "Bearer " + token);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @ApiOperation(value = "get workflow result details", response = String.class)
    @RequestMapping(value = "/workflow/resultsdetails/{historyId}/content/{resultId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowResultsDetails(
            @ApiParam(value = "historyId", required = true) @PathVariable("historyId") String historyId,
            @ApiParam(value = "resultId", required = true) @PathVariable("resultId") String resultId) {
        UserActionLogging.LogAction("Get workflow result details", " historyId : " + historyId + " resultId : " + resultId);

        String url = workflowUrl + "/getWorkflowResultsDetails/" + historyId + "/contents/" + resultId;
        try {
            StringBuilder response = new StringBuilder();
            User user = userInfo.getUser();
            String token = JWTUtil.getJWT(jwtSecret, user.getEmail());
            HTTPUtil.sendAuthorizedHTTP(url, "", response, "GET", "Bearer " + token);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
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
