package eu.hbp.mip.controllers;

import com.google.gson.*;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.hbp.mip.model.*;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.repositories.ModelRepository;
import eu.hbp.mip.utils.HTTPUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import eu.hbp.mip.utils.JWTUtil;
import eu.hbp.mip.utils.UserActionLogging;

import java.io.IOException;
import java.util.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by habfast on 21/04/16.
 */
@RestController
@RequestMapping(value = "/experiments", produces = { APPLICATION_JSON_VALUE })
@Api(value = "/experiments", description = "the experiments API")
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
    public ResponseEntity<String> runWorkflow(@RequestBody ExperimentQuery expQuery) {
        
        Experiment experiment = saveExperiment(expQuery);

        String algoCode = expQuery.getAlgorithms().get(0).getCode();
        List<AlgorithmParam> params = expQuery.getAlgorithms().get(0).getParameters();

        User user = userInfo.getUser();
        String token = JWTUtil.getJWT(jwtSecret, user.getEmail());

        HashMap<String, String> queryMap = new HashMap<String, String>();

        if (params != null) {
            for (AlgorithmParam p : params) {
                queryMap.put(p.getName(), p.getValue());
            }
        }

        String query = gson.toJson(queryMap);
        String url = workflowUrl + "/runWorkflow/" + algoCode;
        // Results are stored in the experiment object

        new Thread(() -> {
            try {
                StringBuilder results = new StringBuilder();
                int code = HTTPUtil.sendAuthorizedHTTP(url, query, results, "POST", "Bearer " + token);
                experiment.setResult("[" + results.toString() + "]");
                experiment.setHasError(code >= 400);
                experiment.setHasServerError(code >= 500);
            } catch (IOException e) {
                //LOGGER.trace("Invalid UUID", e);
                experiment.setHasError(true);
                experiment.setHasServerError(true);
                experiment.setResult(e.getMessage());
            }
            finishExperiment(experiment);
        }).start();

		UserActionLogging.LogAction("create workflow", "no info");
		
        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
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
		
		UserActionLogging.LogAction("Get an experiment ", " uuid : "+ uuid);
		
        return new ResponseEntity<>(gsonOnlyExposed.toJson(experiment.jsonify()), HttpStatus.OK);
    }

    @ApiOperation(value = "get workflow status", response = String.class)
    @RequestMapping(value = "/workflow/status/{historyId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowStatus(
            @ApiParam(value = "historyId", required = true) @PathVariable("historyId") String historyId) {
       
		UserActionLogging.LogAction("Get a workflow status", " historyId : "+ historyId);
		
        String url = workflowUrl + "/getWorkflowStatus/" + historyId;
        try {
            User user = userInfo.getUser();
            String token = JWTUtil.getJWT(jwtSecret, user.getEmail());
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendAuthorizedHTTP(url, "", response, "GET", "Bearer " + token);
            JsonElement element = new JsonParser().parse(response.toString());

            return ResponseEntity.ok(gson.toJson(element));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
		
    }

    // TODO: factorize workflow results
    @ApiOperation(value = "get workflow results", response = String.class)
    @RequestMapping(value = "/workflow/results/{historyId}", method = RequestMethod.GET)
    public ResponseEntity<String> getWorkflowResults(
            @ApiParam(value = "historyId", required = true) @PathVariable("historyId") String historyId) {
        UserActionLogging.LogAction("Get workflow results", " historyId : "+ historyId);
		
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

		UserActionLogging.LogAction("Get workflow result content", " historyId : "+ historyId + " resultId : "+ resultId);
		
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
		UserActionLogging.LogAction("Get workflow result details", " historyId : "+ historyId + " resultId : "+ resultId);

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

		UserActionLogging.LogAction("Mark an experiment as viewed", " uuid : "+ uuid);

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

		UserActionLogging.LogAction("Mark an experiment as shared", " uuid : "+ uuid);
		
        return doMarkExperimentAsShared(uuid, true);
    }

    @ApiOperation(value = "Mark an experiment as unshared", response = Experiment.class)
    @RequestMapping(value = "/{uuid}/markAsUnshared", method = RequestMethod.GET)
    public ResponseEntity<String> markExperimentAsUnshared(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
		UserActionLogging.LogAction("Mark an experiment as unshared", " uuid : "+ uuid);
		
        return doMarkExperimentAsShared(uuid, false);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = { "maxResultCount" })
    public ResponseEntity<String> listExperiments(
            @ApiParam(value = "maxResultCount") @RequestParam int maxResultCount) {

		UserActionLogging.LogAction("List experiments", " maxResultCount : "+ maxResultCount);
		
        return doListExperiments(false, null);
    }

    @ApiOperation(value = "list experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = { "slug", "maxResultCount" })
    public ResponseEntity<String> listExperiments(@ApiParam(value = "slug") @RequestParam("slug") String modelSlug,
            @ApiParam(value = "maxResultCount") @RequestParam("maxResultCount") int maxResultCount) {

		UserActionLogging.LogAction("List experiments", " modelSlug : "+ modelSlug);

        if (maxResultCount <= 0 && (modelSlug == null || "".equals(modelSlug))) {
            return new ResponseEntity<>("You must provide at least a slug or a limit of result",
                    HttpStatus.BAD_REQUEST);
        }

        return doListExperiments(false, modelSlug);
    }

    @ApiOperation(value = "list my experiments", response = Experiment.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET, params = { "mine" })
    public ResponseEntity<String> listMyExperiments(@ApiParam(value = "mine") @RequestParam("mine") boolean mine) {
        UserActionLogging.LogAction("List my experiments", " mine : "+ mine);

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
            for (Iterator<Experiment> it = expList.iterator(); it.hasNext();) {
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

        UserActionLogging.LogAction("Experiment updated (finished)","");
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

        UserActionLogging.LogAction("Saved an experiment", " id : "+experiment.getUuid());

        return experiment;
    }

}
