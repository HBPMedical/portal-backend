package eu.hbp.mip.services;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.models.DTOs.AlgorithmDTO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.models.galaxy.GalaxyWorkflowResult;
import eu.hbp.mip.models.galaxy.PostWorkflowToGalaxyDtoResponse;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.Exceptions.*;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.JsonConverters;
import eu.hbp.mip.utils.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static java.lang.Thread.sleep;

@Service
public class ExperimentService {


    @Value("#{'${services.exareme.queryExaremeUrl}'}")
    private String queryExaremeUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    private static final Gson gson = new Gson();

    private final ActiveUserService activeUserService;
    private final ExperimentRepository experimentRepository;

    public ExperimentService(ActiveUserService activeUserService, ExperimentRepository experimentRepository) {
        this.activeUserService = activeUserService;
        this.experimentRepository = experimentRepository;
    }

    /**
     * The getExperiments will retrieve the experiments from database according to the filters.
     *
     * @param name       is optional, in case it is required to filter the experiments by name
     * @param algorithm  is optional, in case it is required to filter the experiments by algorithm name
     * @param shared     is optional, in case it is required to filter the experiments by shared
     * @param viewed     is optional, in case it is required to filter the experiments by viewed
     * @param includeShared     is optional, in case it is required to retrieve the experiment that is shared
     * @param page       is the page that is required to be retrieve
     * @param size       is the size of each page
     * @param orderBy    is the column that is required to ordered by
     * @param descending is a boolean to determine if the experiments will be order by descending or ascending
     * @param logger    contains username and the endpoint.
     * @return a list of mapped experiments
     */

    public Map getExperiments(Authentication authentication, String name, String algorithm, Boolean shared, Boolean viewed, boolean includeShared, int page, int size, String orderBy, Boolean descending, Logger logger) {

        UserDAO user = activeUserService.getActiveUser();
        logger.LogUserAction("Listing my experiments.");
        if (size > 50)
            throw new BadRequestException("Invalid size input, max size is 50.");
        Specification<ExperimentDAO> spec;
        if(!authenticationIsEnabled  || ClaimUtils.validateAccessRightsOnExperiments(user.getUsername(), authentication.getAuthorities(), logger))
        {
            spec = Specification
                    .where(new ExperimentSpecifications.ExperimentWithName(name))
                    .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                    .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                    .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                    .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
        }
        else {
            spec = Specification
                    .where(new ExperimentSpecifications.MyExperiment(user.getUsername()))
                    .or(new ExperimentSpecifications.SharedExperiment(includeShared))
                    .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                    .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                    .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                    .and(new ExperimentSpecifications.ExperimentWithName(name))
                    .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
        }
        Pageable paging = PageRequest.of(page, size);
        Page<ExperimentDAO> pageExperiments = experimentRepository.findAll(spec, paging);
        List<ExperimentDAO> experimentDAOs = pageExperiments.getContent();

        if (experimentDAOs.isEmpty())
            throw new NoContent("No experiment found with the filters provided.");

        List<ExperimentDTO> experimentDTOs = new ArrayList<>();
        experimentDAOs.forEach(experimentDAO -> experimentDTOs.add(experimentDAO.convertToDTO(false)));

        Map<String, Object> response = new HashMap<>();
        response.put("experiments", experimentDTOs);
        response.put("currentPage", pageExperiments.getNumber());
        response.put("totalExperiments", pageExperiments.getTotalElements());
        response.put("totalPages", pageExperiments.getTotalPages());

        return response;
    }

    /**
     * The getExperiment will retrieve the experiment from database according to the input uuid
     *
     * @param uuid     is the id of the experiment to be retrieved
     * @param logger    contains username and the endpoint.
     * @return the experiment information that was retrieved from the database
     */
    public ExperimentDTO getExperiment(Authentication authentication, String uuid, Logger logger) {

        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();

        logger.LogUserAction("Loading Experiment with uuid : " + uuid);

        experimentDAO = loadExperiment(uuid, logger);
        if (
                !experimentDAO.isShared()
                && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername())
                && authenticationIsEnabled
                && ClaimUtils.validateAccessRightsOnExperiments(user.getUsername(), authentication.getAuthorities(), logger)
        ) {
            logger.LogUserAction("Accessing Experiment is unauthorized.");
            throw new UnauthorizedException("You don't have access to the experiment.");
        }
        ExperimentDTO experimentDTO = experimentDAO.convertToDTO(true);
        logger.LogUserAction("Experiment was Loaded with uuid : " + uuid + ".");

        return experimentDTO;
    }

    /**
     * The createExperiment will create and save an experiment in the database.
     *
     * @param authentication is the role of the user
     * @param experimentDTO  is the experiment information
     * @param logger    contains username and the endpoint.
     * @return the experiment information which was created
     */
    public ExperimentDTO createExperiment(Authentication authentication, ExperimentDTO experimentDTO, Logger logger) {
        UserDAO user = activeUserService.getActiveUser();

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, logger);

        // Get the type and name of algorithm
        String algorithmType = experimentDTO.getAlgorithm().getType();

        if(algorithmType == null){
            logger.LogUserAction("Please provide algorithm type.");
            throw new BadRequestException("Please provide algorithm type.");
        }

        algorithmParametersLogging(experimentDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, logger);
            ClaimUtils.validateAccessRightsOnDatasets(user.getUsername(), authentication.getAuthorities(), experimentDatasets, logger);
        }

        // Run with the appropriate engine
        if (algorithmType.equals("workflow")) {
            logger.LogUserAction("Algorithm runs on Galaxy.");
            return runGalaxyWorkflow(experimentDTO, logger);
        } else {
            logger.LogUserAction("Algorithm runs on Exareme.");
            return createExaremeExperiment(experimentDTO, logger);
        }
    }

    /**
     * The createTransientExperiment will run synchronous a transient experiment into exareme and provide results
     *
     * @param authentication is the role of the user
     * @param experimentDTO  is the experiment information
     * @param logger    contains username and the endpoint.
     * @return the experiment information which was created
     */
    public ExperimentDTO createTransientExperiment(Authentication authentication, ExperimentDTO experimentDTO, Logger logger) {
        UserDAO user = activeUserService.getActiveUser();

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, logger);

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithm().getParameters();

        // Get the type and name of algorithm
        String algorithmName = experimentDTO.getAlgorithm().getName();

        algorithmParametersLogging(experimentDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, logger);
            ClaimUtils.validateAccessRightsOnDatasets(user.getUsername(), authentication.getAuthorities(), experimentDatasets, logger);
        }

        String body = gson.toJson(algorithmParameters);
        String url = queryExaremeUrl + "/" + algorithmName;
        logger.LogUserAction("url: " + url + ", body: " + body);

        logger.LogUserAction("Completed, returning: " + experimentDTO.toString());

        // Results are stored in the experiment object
        ExaremeResult exaremeResult = runExaremeExperiment(url, body, experimentDTO);

        logger.LogUserAction("Experiment with uuid: " + experimentDTO.getUuid() + "gave response code: " + exaremeResult.getCode() + " and result: " + exaremeResult.getResults());

        experimentDTO.setResult((exaremeResult.getCode() >= 400) ? null : exaremeResult.getResults());
        experimentDTO.setStatus((exaremeResult.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);

        return experimentDTO;
    }

    /**
     * The updateExperiment will update the experiment's properties
     *
     * @param uuid          is the id of the experiment to be updated
     * @param experimentDTO is the experiment information to be updated
     * @param logger    contains username and the endpoint.
     */
    public ExperimentDTO updateExperiment(String uuid, ExperimentDTO experimentDTO, Logger logger) {
        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();
        logger.LogUserAction("Updating experiment with uuid : " + uuid + ".");

        experimentDAO = loadExperiment(uuid, logger);

        //Verify (PATCH) /experiments non editable fields.
        verifyPatchExperimentNonEditableFields(experimentDTO, logger);

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()))
            throw new UnauthorizedException("You don't have access to the experiment.");

        if (experimentDTO.getName() != null && experimentDTO.getName().length() != 0)
            experimentDAO.setName(experimentDTO.getName());

        if (experimentDTO.getShared() != null)
            experimentDAO.setShared(experimentDTO.getShared());

        if (experimentDTO.getViewed() != null)
            experimentDAO.setViewed(experimentDTO.getViewed());

        experimentDAO.setUpdated(new Date());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction("Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        logger.LogUserAction("Updated experiment with uuid : " + uuid + ".");

        experimentDTO = experimentDAO.convertToDTO(true);
        return experimentDTO;
    }

    /**
     * The deleteExperiment will delete an experiment from the database
     *
     * @param uuid     is the id of the experiment to be deleted
     * @param logger    contains username and the endpoint.
     */
    public void deleteExperiment(String uuid, Logger logger) {
        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();
        logger.LogUserAction("Deleting experiment with uuid : " + uuid + ".");

        experimentDAO = loadExperiment(uuid, logger);

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()))
            throw new UnauthorizedException("You don't have access to the experiment.");

        try {
            experimentRepository.delete(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction("Attempted to delete an experiment to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        logger.LogUserAction("Deleted experiment with uuid : " + uuid + ".");
    }

    //    /* -------------------------------  PRIVATE METHODS  ----------------------------------------------------*/

    private void checkPostExperimentProperInput(ExperimentDTO experimentDTO, Logger logger) {

        boolean properInput =
                experimentDTO.getShared() == null
                        && experimentDTO.getViewed() == null
                        && experimentDTO.getCreated() == null
                        && experimentDTO.getCreatedBy() == null
                        && experimentDTO.getResult() == null
                        && experimentDTO.getStatus() == null
                        && experimentDTO.getUuid() == null;

        if (!properInput) {
            logger.LogUserAction( "Invalid input.");
            throw new BadRequestException("Please provide proper input.");
        }
    }

    private void verifyPatchExperimentNonEditableFields(ExperimentDTO experimentDTO, Logger logger) {
        if (experimentDTO.getUuid() != null ) {
            logger.LogUserAction( "Uuid is not editable.");
            throw new BadRequestException("Uuid is not editable.");
        }

        if (experimentDTO.getAlgorithm() != null ) {
            logger.LogUserAction( "Algorithm is not editable.");
            throw new BadRequestException("Algorithm is not editable.");
        }

        if (experimentDTO.getCreated() != null) {
            logger.LogUserAction( "Created is not editable.");
            throw new BadRequestException("Created is not editable.");
        }

        if (experimentDTO.getCreatedBy() != null) {
            logger.LogUserAction( "CreatedBy is not editable.");
            throw new BadRequestException("CreatedBy is not editable.");
        }

        if (experimentDTO.getUpdated() != null) {
            logger.LogUserAction( "Updated is not editable.");
            throw new BadRequestException("Updated is not editable.");
        }

        if (experimentDTO.getFinished() != null) {
            logger.LogUserAction( "Finished is not editable.");
            throw new BadRequestException("Finished is not editable.");
        }

        if (experimentDTO.getResult() != null) {
            logger.LogUserAction( "Result is not editable.");
            throw new BadRequestException("Result is not editable.");
        }

        if (experimentDTO.getStatus() != null) {
            logger.LogUserAction( "Status is not editable.");
            throw new BadRequestException("Status is not editable.");
        }
    }

    private void algorithmParametersLogging(ExperimentDTO experimentDTO, Logger logger) {
        String algorithmName = experimentDTO.getAlgorithm().getName();
        StringBuilder parametersLogMessage = new StringBuilder(", Parameters:\n");
        experimentDTO.getAlgorithm().getParameters().forEach(
                params -> parametersLogMessage
                        .append("  ")
                        .append(params.getLabel())
                        .append(" -> ")
                        .append(params.getValue())
                        .append("\n"));
        logger.LogUserAction("Executing " + algorithmName + parametersLogMessage);
    }
    
    /**
     * The getDatasetFromExperimentParameters will retrieve the dataset from the experiment parameters
     *
     * @param experimentDTO is the experiment information
     * @param logger    contains username and the endpoint.
     * @return the dataset from the experiment
     */
    private String getDatasetFromExperimentParameters(ExperimentDTO experimentDTO, Logger logger) {

        String experimentDatasets = null;
        for (AlgorithmDTO.AlgorithmParamDTO parameter : experimentDTO.getAlgorithm().getParameters()) {
            if (parameter.getLabel().equals("dataset")) {
                experimentDatasets = parameter.getValue();
                break;
            }
        }

        if (experimentDatasets == null || experimentDatasets.equals("")) {
            logger.LogUserAction("A dataset should be specified to run an algorithm.");
            throw new BadRequestException("Please provide at least one dataset to run the algorithm.");
        }
        return experimentDatasets;
    }

    /**
     * The loadExperiment access the database and load the information of a specific experiment
     *
     * @param uuid is the id of the experiment to be retrieved
     * @return the experiment information that was retrieved from database
     */
    private ExperimentDAO loadExperiment(String uuid, Logger logger) {
        UUID experimentUuid;
        ExperimentDAO experimentDAO;

        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (Exception e) {
            logger.LogUserAction( e.getMessage());
            throw new BadRequestException(e.getMessage());
        }

        experimentDAO = experimentRepository.findByUuid(experimentUuid);
        if (experimentDAO == null) {
            logger.LogUserAction( "Experiment with uuid : " + uuid + "was not found.");
            throw new ExperimentNotFoundException("Experiment with uuid : " + uuid + " was not found.");
        }

        return experimentDAO;
    }

    /**
     * The createExperimentInTheDatabase will insert a new experiment in the database according to the given experiment information
     *
     * @param experimentDTO is the experiment information to inserted in the database
     * @return the experiment information that was inserted into the database
     * @Note In the database there will be stored Algorithm Details that is the whole information about the algorithm
     * and an Algorithm column that is required for the filtering with algorithm name  in the GET /experiments.
     */
    private ExperimentDAO createExperimentInTheDatabase(ExperimentDTO experimentDTO, Logger logger) {
        UserDAO user = activeUserService.getActiveUser();

        ExperimentDAO experimentDAO = new ExperimentDAO();
        experimentDAO.setUuid(UUID.randomUUID());
        experimentDAO.setCreatedBy(user);
        experimentDAO.setAlgorithm(JsonConverters.convertObjectToJsonString(experimentDTO.getAlgorithm()));
        experimentDAO.setAlgorithmId(experimentDTO.getAlgorithm().getName());
        experimentDAO.setName(experimentDTO.getName());
        experimentDAO.setStatus(ExperimentDAO.Status.pending);

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction("Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        logger.LogUserAction(" id : " + experimentDAO.getUuid());
        logger.LogUserAction(" algorithm : " + experimentDAO.getAlgorithm());
        logger.LogUserAction(" name : " + experimentDAO.getName());
        return experimentDAO;
    }

    private void saveExperiment(ExperimentDAO experimentDAO, Logger logger) {

        logger.LogUserAction(" id : " + experimentDAO.getUuid());
        logger.LogUserAction(" algorithm : " + experimentDAO.getAlgorithm());
        logger.LogUserAction(" name : " + experimentDAO.getName());
        logger.LogUserAction(" historyId : " + experimentDAO.getWorkflowHistoryId());
        logger.LogUserAction(" status : " + experimentDAO.getStatus());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction("Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        logger.LogUserAction("Saved experiment");
    }

    private void finishExperiment(ExperimentDAO experimentDAO, Logger logger) {
        experimentDAO.setFinished(new Date());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction( "Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }
    }

    /* --------------------------------------  EXAREME CALLS ---------------------------------------------------------*/

    /**
     * The createExaremeExperiment will POST the algorithm to the exareme client
     *
     * @param experimentDTO is the request with the experiment information
     * @param logger    contains username and the endpoint.
     * @return the experiment information that was retrieved from exareme
     */
    public ExperimentDTO createExaremeExperiment(ExperimentDTO experimentDTO, Logger logger) {

        logger.LogUserAction("Running the algorithm...");

        ExperimentDAO experimentDAO = createExperimentInTheDatabase(experimentDTO, logger);
        logger.LogUserAction("Created experiment with uuid :" + experimentDAO.getUuid());

        // Run the 1st algorithm from the list
        String algorithmName = experimentDTO.getAlgorithm().getName();

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithm().getParameters();

        String body = gson.toJson(algorithmParameters);
        String url = queryExaremeUrl + "/" + algorithmName;
        logger.LogUserAction("url: " + url + ", body: " + body);

        logger.LogUserAction("Completed, returning: " + experimentDTO.toString());

        logger.LogUserAction("Starting exareme execution thread");
        ExperimentDTO finalExperimentDTO = experimentDTO;
        new Thread(() -> {

            // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogUserAction!
            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread named :" + Thread.currentThread().getName() + " with id :" + Thread.currentThread().getId() + " started!");

            try {
                // Results are stored in the experiment object
                ExaremeResult exaremeResult = runExaremeExperiment(url, body, finalExperimentDTO);

                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Experiment with uuid: " + experimentDAO.getUuid() + "gave response code: " + exaremeResult.getCode() + " and result: " + exaremeResult.getResults());

                experimentDAO.setResult((exaremeResult.getCode() >= 400) ? null : JsonConverters.convertObjectToJsonString(exaremeResult.getResults()));
                experimentDAO.setStatus((exaremeResult.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "There was an exception: " + e.getMessage());

                experimentDAO.setStatus(ExperimentDAO.Status.error);
            }

            finishExperiment(experimentDAO, logger);
            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Finished the experiment: " + experimentDAO.toString());
        }).start();
        experimentDTO = experimentDAO.convertToDTO(true);
        return experimentDTO;
    }

    /**
     * The runExaremeExperiment will run to exareme the experiment
     *
     * @param url           is the url that contain the results of the experiment
     * @param body          is the parameters of the algorithm
     * @param experimentDTO is the experiment information to be executed in the exareme
     * @return the result of exareme as well as the http status that was retrieved
     */
    public ExaremeResult runExaremeExperiment(String url, String body, ExperimentDTO experimentDTO) {

        StringBuilder results = new StringBuilder();
        int code;
        try {
            code = HTTPUtil.sendPost(url, body, results);
        } catch (Exception e) {
            throw new InternalServerError("Error occurred : " + e.getMessage());
        }
        Logger.LogExperimentAction(experimentDTO.getName(), experimentDTO.getUuid(), "Algorithm finished with code: " + code);

        // Results are stored in the experiment object
        ExperimentDTO experimentDTOWithOnlyResult = JsonConverters.convertJsonStringToObject(String.valueOf(results), ExperimentDTO.class);
        List<ExperimentDTO.ResultDTO> resultDTOS = experimentDTOWithOnlyResult.getResult();
        return new ExaremeResult(code, resultDTOS);
    }


    /* ---------------------------------------  GALAXY CALLS ---------------------------------------------------------*/


    /**
     * The runWorkflow will POST the algorithm to the galaxy client
     *
     * @param experimentDTO is the request with the experiment information
     * @return the response to be returned
     */
    public ExperimentDTO runGalaxyWorkflow(ExperimentDTO experimentDTO, Logger logger) {
        logger.LogUserAction("Running a workflow...");

        ExperimentDAO experimentDAO = createExperimentInTheDatabase(experimentDTO, logger);
        logger.LogUserAction("Created experiment with uuid :" + experimentDAO.getUuid());


        // Run the 1st algorithm from the list
        String workflowId = experimentDTO.getAlgorithm().getName();

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithm().getParameters();

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
            logger.LogUserAction("Could not find algorithm code: " + workflowId);
            throw new BadRequestException("Could not find galaxy algorithm.");
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
        logger.LogUserAction("Running Galaxy workflow with id: " + workflow.getId());

        // Call Galaxy to run the workflow
        Call<PostWorkflowToGalaxyDtoResponse> call = service.postWorkflowToGalaxy(workflow.getId(), galaxyApiKey, requestBodyJson);
        try {
            Response<PostWorkflowToGalaxyDtoResponse> response = call.execute();

            if (response.code() == 200) {       // Call succeeded
                String responseBody = gson.toJson(response.body());
                logger.LogUserAction("Response: " + responseBody);

                String historyId = (String) new JSONObject(responseBody).get("history_id");
                experimentDAO.setWorkflowHistoryId(historyId);
                experimentDAO.setStatus(ExperimentDAO.Status.success);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                logger.LogUserAction("Error Response: " + msgErr);

                // Values are read from streams.
                JSONObject jObjectError = new JSONObject(msgErr);
                String errMsg = jObjectError.get("err_msg").toString();

                experimentDTO.setStatus((response.code() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            }

        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            experimentDAO.setStatus(ExperimentDAO.Status.error);
        }
        saveExperiment(experimentDAO, logger);

        // Start the process of fetching the status
        updateWorkflowExperiment(experimentDAO, logger);

        logger.LogUserAction("Run workflow completed!");

        experimentDTO = experimentDAO.convertToDTO(true);
        return experimentDTO;
    }


    /**
     * This method creates a thread that will fetch the workflow result when it is ready
     *
     * @param experimentDAO will be used to fetch it's workflow status, it should have the workflowHistoryId initialized
     *                      and the result should not already be fetched
     */
    public void updateWorkflowExperiment(ExperimentDAO experimentDAO, Logger logger) {

        if (experimentDAO == null) {
            logger.LogUserAction("The experiment does not exist.");
            return;
        }

        logger.LogUserAction(" Experiment id : " + experimentDAO.getUuid());
        if (experimentDAO.getWorkflowHistoryId() == null) {
            logger.LogUserAction("History Id does not exist.");
            return;
        }

        logger.LogUserAction("Starting Thread...");
        new Thread(() -> {
            while (true) {
                // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogExperimentAction!
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread is running...");

                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Sleep was disrupted: " + e.getMessage());
                }

                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Fetching status for experiment Id: " + experimentDAO.getUuid());

                String state = getWorkflowStatus(experimentDAO);
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "State is: " + state);

                switch (state) {
                    case "running":
                        // Do nothing, when the experiment is created the status is set to running
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Workflow is still running.");
                        break;

                    case "completed":
                        // Get only the job result that is visible
                        List<GalaxyWorkflowResult> workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Results are: " + workflowJobsResults.toString());

                        boolean resultFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getVisible()) {
                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Visible result are: " + jobResult.getId());

                                String result = getWorkflowResultBody(experimentDAO, jobResult.getId());

                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                } else {
                                    experimentDAO.setResult("[" + result + "]");
                                    experimentDAO.setStatus(ExperimentDAO.Status.success);
                                    resultFound = true;
                                }
                            }
                        }

                        if (!resultFound) {      // If there is no visible result
                            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No visible result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }

                        finishExperiment(experimentDAO, logger);
                        break;

                    case "error":
                        // Get the job result that failed
                        workflowJobsResults = getWorkflowResults(experimentDAO);
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Error results are: " + workflowJobsResults.toString());

                        boolean failedJobFound = false;
                        for (GalaxyWorkflowResult jobResult : workflowJobsResults) {
                            if (jobResult.getState().equals("error")) {
                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Failed job is: " + jobResult.getId());

                                String result = getWorkflowJobError(jobResult.getId(), experimentDAO);

                                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Job result: " + result);
                                if (result == null) {
                                    experimentDAO.setStatus(ExperimentDAO.Status.error);
                                }
                                experimentDAO.setStatus(ExperimentDAO.Status.error);
                                failedJobFound = true;
                            }
                        }

                        if (!failedJobFound) {      // If there is no visible failed job
                            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No failed result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }
                        finishExperiment(experimentDAO, logger);
                        break;

                    default:        // InternalError or unexpected result
                        Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "An unexpected error occurred.");
                        experimentDAO.setStatus(ExperimentDAO.Status.error);
                        finishExperiment(experimentDAO, logger);
                        break;
                }

                // If result exists return
                if (experimentDAO.getResult() != null) {
                    Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "ResultDTO exists: " + experimentDAO.getResult());
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
        Logger.LogExperimentAction(experimentName, experimentId, " History Id : " + historyId);

        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call = service.getWorkflowStatusFromGalaxy(historyId, galaxyApiKey);

        String result;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return "internalError";
            }
            result = new Gson().toJson(response.body());
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + result);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        String state;
        try {
            JSONObject resultJson = new JSONObject(result);
            state = resultJson.getString("state");
        } catch (JSONException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return "internalError";
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
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
        Logger.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<List<GalaxyWorkflowResult>> call = service.getWorkflowResultsFromGalaxy(historyId, galaxyApiKey);

        List<GalaxyWorkflowResult> getGalaxyWorkflowResultList;
        try {
            Response<List<GalaxyWorkflowResult>> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            getGalaxyWorkflowResultList = response.body();
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + response.body());

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, " An exception happened: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
        return getGalaxyWorkflowResultList;

    }

    /**
     * @param experimentDAO The experiment of the workflow
     * @param contentId     the id of the job result that we want
     * @return the result of the specific workflow job, null if there was an error
     */
    public String getWorkflowResultBody(ExperimentDAO experimentDAO, String contentId) {

        String historyId = experimentDAO.getWorkflowHistoryId();
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logger.LogExperimentAction(experimentName, experimentId, " historyId : " + historyId);

        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> call =
                service.getWorkflowResultsBodyFromGalaxy(historyId, contentId, galaxyApiKey);

        String resultJson;
        try {
            Response<Object> response = call.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, " Response code: "
                        + response.code() + "" + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }
            resultJson = new Gson().toJson(response.body());
            Logger.LogExperimentAction(experimentName, experimentId, " ResultDTO: " + resultJson);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId,
                    " An exception happened: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, " Completed!");
        return resultJson;
    }


    /**
     * @param jobId the id of the workflow job that failed
     * @return the error that was produced or null if an error occurred
     */
    public String getWorkflowJobError(String jobId, ExperimentDAO experimentDAO) {
        String experimentName = experimentDAO.getName();
        UUID experimentId = experimentDAO.getUuid();

        Logger.LogExperimentAction(experimentName, experimentId, " jobId : " + jobId);
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        Call<Object> callError = service.getErrorMessageOfWorkflowFromGalaxy(jobId, galaxyApiKey);

        String fullError;
        String returnError;
        try {
            Response<Object> response = callError.execute();
            if (response.code() >= 400) {
                Logger.LogExperimentAction(experimentName, experimentId, "Response code: "
                        + response.code() + " with body: " + (response.errorBody() != null ? response.errorBody().string() : " "));
                return null;
            }

            // Parsing the stderr of the job that failed
            String jsonString = new Gson().toJson(response.body());
            JsonElement jsonElement = new JsonParser().parse(jsonString);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            fullError = rootObject.get("stderr").getAsString();
            Logger.LogExperimentAction(experimentName, experimentId, "Error: " + fullError);

            String[] arrOfStr = fullError.split("ValueError", 0);
            String specError = arrOfStr[arrOfStr.length - 1];
            returnError = specError.substring(1);
            Logger.LogExperimentAction(experimentName, experimentId, "Parsed Error: " + returnError);

        } catch (IOException e) {
            Logger.LogExperimentAction(experimentName, experimentId, "Exception: " + e.getMessage());
            return null;
        }

        Logger.LogExperimentAction(experimentName, experimentId, "Completed successfully!");

        return returnError;
    }

    static class ExaremeResult {
        private int code;
        private List<ExperimentDTO.ResultDTO> results;

        public ExaremeResult(int code, List<ExperimentDTO.ResultDTO> results) {
            this.code = code;
            this.results = results;
        }

        public int getCode() {
            return code;
        }

        public List<ExperimentDTO.ResultDTO> getResults() {
            return results;
        }
    }
}
