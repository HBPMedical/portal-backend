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
import eu.hbp.mip.utils.Logging;
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

    private ActiveUserService activeUserService;
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
     * @param endpoint   is the endpoint that called the function
     * @return a list of mapped experiments
     */

    public Map getExperiments(String name, String algorithm, Boolean shared, Boolean viewed, boolean includeShared, int page, int size, String orderBy, Boolean descending, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Listing my experiments.");
        if (size > 50)
            throw new BadRequestException("Invalid size input, max size is 50.");
        /*TODO:if(role == master){
        Specification<ExperimentDAO> spec = Specification
                .where(new ExperimentSpecifications.ExperimentWithName(name))
                .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
        }
        else
         */
        Specification<ExperimentDAO> spec = Specification
                .where(new ExperimentSpecifications.MyExperiment(user.getUsername()))
                .or(new ExperimentSpecifications.SharedExperiment(includeShared))
                .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                .and(new ExperimentSpecifications.ExperimentWithName(name))
                .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));

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
     * @param endpoint is the endpoint that called the function
     * @return the experiment information that was retrieved from the database
     */
    public ExperimentDTO getExperiment(String uuid, String endpoint) {

        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();

        Logging.LogUserAction(user.getUsername(), endpoint, "Loading Experiment with uuid : " + uuid);

        experimentDAO = loadExperiment(uuid, endpoint);
        //TODO: if (!experimentDAO.isShared() && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()) && if not master) {
        if (!experimentDAO.isShared() && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()) ) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Accessing Experiment is unauthorized.");
            throw new UnauthorizedException("You don't have access to the experiment.");
        }
        ExperimentDTO experimentDTO = experimentDAO.convertToDTO(true);
        Logging.LogUserAction(user.getUsername(), endpoint, "Experiment was Loaded with uuid : " + uuid + ".");

        return experimentDTO;
    }

    /**
     * The createExperiment will create and save an experiment in the database.
     *
     * @param authentication is the role of the user
     * @param experimentDTO  is the experiment information
     * @param endpoint       is the endpoint that called the function
     * @return the experiment information which was created
     */
    public ExperimentDTO createExperiment(Authentication authentication, ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, endpoint);

        // Get the type and name of algorithm
        String algorithmType = experimentDTO.getAlgorithmDetails().getType();

        if(algorithmType == null){
            Logging.LogUserAction(user.getUsername(), endpoint, "Please provide algorithm type.");
            throw new BadRequestException("Please provide algorithm type.");
        }

        algorithmParametersLogging(experimentDTO, endpoint);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, endpoint);
            ClaimUtils.validateAccessRightsOnDatasets(user.getUsername(), authentication.getAuthorities(), experimentDatasets, endpoint);
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

    /**
     * The createTransientExperiment will run synchronous a transient experiment into exareme and provide results
     *
     * @param authentication is the role of the user
     * @param experimentDTO  is the experiment information
     * @param endpoint       is the endpoint that called the function
     * @return the experiment information which was created
     */
    public ExperimentDTO createTransientExperiment(Authentication authentication, ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, endpoint);

        // Get the parameters
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParameters
                = experimentDTO.getAlgorithmDetails().getParameters();

        // Get the type and name of algorithm
        String algorithmName = experimentDTO.getAlgorithmDetails().getName();

        if (!allowedTransientAlgorithms(algorithmName)) {
            Logging.LogUserAction(user.getUsername(), endpoint,
                    "Not proper algorithm.");
            throw new BadRequestException("Please provide proper algorithm.");
        }

        algorithmParametersLogging(experimentDTO, endpoint);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, endpoint);
            ClaimUtils.validateAccessRightsOnDatasets(user.getUsername(), authentication.getAuthorities(), experimentDatasets, endpoint);
        }

        String body = gson.toJson(algorithmParameters);
        String url = queryExaremeUrl + "/" + algorithmName;
        Logging.LogUserAction(user.getUsername(), endpoint, "url: " + url + ", body: " + body);

        Logging.LogUserAction(user.getUsername(), endpoint,
                "Completed, returning: " + experimentDTO.toString());

        // Results are stored in the experiment object
        ExaremeResult exaremeResult = runExaremeExperiment(url, body, experimentDTO);

        Logging.LogUserAction(user.getUsername(), endpoint, "Experiment with uuid: " + experimentDTO.getUuid() + "gave response code: " + exaremeResult.getCode() + " and result: " + exaremeResult.getResults());

        experimentDTO.setResult((exaremeResult.getCode() >= 400) ? null : exaremeResult.getResults());
        experimentDTO.setStatus((exaremeResult.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);

        return experimentDTO;
    }

    /**
     * The updateExperiment will update the experiment's properties
     *
     * @param uuid          is the id of the experiment to be updated
     * @param experimentDTO is the experiment information to be updated
     * @param endpoint      is the endpoint that called the function
     * @return the updated experiment information
     */
    public ExperimentDTO updateExperiment(String uuid, ExperimentDTO experimentDTO, String endpoint) {
        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Updating experiment with uuid : " + uuid + ".");

        experimentDAO = loadExperiment(uuid, endpoint);

        //Verify (PATCH) /experiments non editable fields.
        verifyPatchExperimentNonEditableFields(uuid, experimentDTO, experimentDAO, endpoint);

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
            Logging.LogUserAction(user.getUsername(), endpoint, "Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        Logging.LogUserAction(user.getUsername(), endpoint, "Updated experiment with uuid : " + uuid + ".");

        experimentDTO = experimentDAO.convertToDTO(true);
        return experimentDTO;
    }

    /**
     * The deleteExperiment will delete an experiment from the database
     *
     * @param uuid     is the id of the experiment to be deleted
     * @param endpoint is the endpoint that called the function
     */
    public void deleteExperiment(String uuid, String endpoint) {
        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Deleting experiment with uuid : " + uuid + ".");

        experimentDAO = loadExperiment(uuid, endpoint);

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.getUsername()))
            throw new UnauthorizedException("You don't have access to the experiment.");

        try {
            experimentRepository.delete(experimentDAO);
        } catch (Exception e) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Attempted to delete an experiment to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        Logging.LogUserAction(user.getUsername(), endpoint, "Deleted experiment with uuid : " + uuid + ".");
    }

    //    /* -------------------------------  PRIVATE METHODS  ----------------------------------------------------*/

    private void checkPostExperimentProperInput(ExperimentDTO experimentDTO, String endpoint) {

        boolean properInput =
                experimentDTO.getShared() == null
                        && experimentDTO.getViewed() == null
                        && experimentDTO.getCreated() == null
                        && experimentDTO.getCreatedBy() == null
                        && experimentDTO.getResult() == null
                        && experimentDTO.getStatus() == null
                        && experimentDTO.getUuid() == null;

        if (!properInput) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Invalid input.");
            throw new BadRequestException("Please provide proper input.");
        }
    }

    private boolean allowedTransientAlgorithms(String algorithmName) {
        List<String> properAlgorithms = new ArrayList<>();
        properAlgorithms.add("MULTIPLE_HISTOGRAMS");
        properAlgorithms.add("DESCRIPTIVE_STATS");
        return properAlgorithms.contains(algorithmName);
    }

    private void verifyPatchExperimentNonEditableFields(String uuid, ExperimentDTO experimentDTO, ExperimentDAO experimentDAO, String endpoint) {
        if (experimentDTO.getUuid() != null ) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Uuid is not editable.");
            throw new BadRequestException("Uuid is not editable.");
        }

        if (experimentDTO.getAlgorithm() != null ) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Algorithm is not editable.");
            throw new BadRequestException("Algorithm is not editable.");
        }

        if (experimentDTO.getCreated() != null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "CreatedBy is not editable.");
            throw new BadRequestException("CreatedBy is not editable.");
        }

        if (experimentDTO.getAlgorithmDetails() != null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "AlgorithmDetails is not editable.");
            throw new BadRequestException("AlgorithmDetails is not editable.");
        }

        if (experimentDTO.getCreated() != null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Created is not editable.");
            throw new BadRequestException("Created is not editable.");
        }

        if (experimentDTO.getResult() != null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Status is not editable.");
            throw new BadRequestException("Status is not editable.");
        }

        if (experimentDTO.getStatus() != null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Status is not editable.");
            throw new BadRequestException("Status is not editable.");
        }
    }

    private void algorithmParametersLogging(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();
        String algorithmName = experimentDTO.getAlgorithm();
        StringBuilder parametersLogMessage = new StringBuilder(", Parameters:\n");
        experimentDTO.getAlgorithmDetails().getParameters().forEach(
                params -> parametersLogMessage
                        .append("  ")
                        .append(params.getLabel())
                        .append(" -> ")
                        .append(params.getValue())
                        .append("\n"));
        Logging.LogUserAction(user.getUsername(), endpoint, "Executing " + algorithmName + parametersLogMessage);
    }

    /**
     * The getDatasetFromExperimentParameters will retrieve the dataset from the experiment parameters
     *
     * @param experimentDTO is the experiment information
     * @param endpoint      is the endpoint that called the function
     * @return the dataset from the experiment
     */
    private String getDatasetFromExperimentParameters(ExperimentDTO experimentDTO, String endpoint) {

        String experimentDatasets = null;
        for (AlgorithmDTO.AlgorithmParamDTO parameter : experimentDTO.getAlgorithmDetails().getParameters()) {
            if (parameter.getLabel().equals("dataset")) {
                experimentDatasets = parameter.getValue();
                break;
            }
        }

        if (experimentDatasets == null || experimentDatasets.equals("")) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint,
                    "A dataset should be specified to run an algorithm.");
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
    private ExperimentDAO loadExperiment(String uuid, String endpoint) {
        UUID experimentUuid;
        ExperimentDAO experimentDAO;

        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (Exception e) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, e.getMessage());
            throw new BadRequestException(e.getMessage());
        }

        experimentDAO = experimentRepository.findByUuid(experimentUuid);
        if (experimentDAO == null) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Experiment with uuid : " + uuid + "was not found.");
            throw new ExperimentNotFoundException("Experiment with uuid : " + uuid + " was not found.");
        }

        return experimentDAO;
    }

    /**
     * The createExperimentInTheDatabase will insert a new experiment in the database according to the given experiment information
     *
     * @param experimentDTO is the experiment information to inserted in the database
     * @return the experiment information that was inserted into the database
     */
    private ExperimentDAO createExperimentInTheDatabase(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

        ExperimentDAO experimentDAO = new ExperimentDAO();
        experimentDAO.setUuid(UUID.randomUUID());
        experimentDAO.setCreatedBy(user);
        experimentDAO.setAlgorithmDetails(JsonConverters.convertObjectToJsonString(experimentDTO.getAlgorithmDetails()));
        experimentDAO.setAlgorithm(experimentDTO.getAlgorithm());
        experimentDAO.setName(experimentDTO.getName());
        experimentDAO.setStatus(ExperimentDAO.Status.pending);

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        Logging.LogUserAction(user.getUsername(), endpoint, " id : " + experimentDAO.getUuid());
        Logging.LogUserAction(user.getUsername(), endpoint, " algorithms : " + experimentDAO.getAlgorithmDetails());
        Logging.LogUserAction(user.getUsername(), endpoint, " name : " + experimentDAO.getName());
        return experimentDAO;
    }

    private void saveExperiment(ExperimentDAO experimentDAO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

        Logging.LogUserAction(user.getUsername(), endpoint, " id : " + experimentDAO.getUuid());
        Logging.LogUserAction(user.getUsername(), endpoint, " algorithms : " + experimentDAO.getAlgorithmDetails());
        Logging.LogUserAction(user.getUsername(), endpoint, " name : " + experimentDAO.getName());
        Logging.LogUserAction(user.getUsername(), endpoint, " historyId : " + experimentDAO.getWorkflowHistoryId());
        Logging.LogUserAction(user.getUsername(), endpoint, " status : " + experimentDAO.getStatus());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            Logging.LogUserAction(user.getUsername(), endpoint, "Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        Logging.LogUserAction(user.getUsername(), endpoint, "Saved experiment");
    }

    private void finishExperiment(ExperimentDAO experimentDAO, String endpoint) {
        experimentDAO.setFinished(new Date());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), endpoint, "Attempted to save changes to database but an error ocurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }
    }

    /* --------------------------------------  EXAREME CALLS ---------------------------------------------------------*/

    /**
     * The createExaremeExperiment will POST the algorithm to the exareme client
     *
     * @param experimentDTO is the request with the experiment information
     * @param endpoint      is the endpoint that called the function
     * @return the experiment information that was retrieved from exareme
     */
    public ExperimentDTO createExaremeExperiment(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

        Logging.LogUserAction(user.getUsername(), endpoint, "Running the algorithm...");

        ExperimentDAO experimentDAO = createExperimentInTheDatabase(experimentDTO, endpoint);
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
        ExperimentDTO finalExperimentDTO = experimentDTO;
        new Thread(() -> {

            // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogUserAction!
            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread named :" + Thread.currentThread().getName() + " with id :" + Thread.currentThread().getId() + " started!");

            try {
                // Results are stored in the experiment object
                ExaremeResult exaremeResult = runExaremeExperiment(url, body, finalExperimentDTO);

                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Experiment with uuid: " + experimentDAO.getUuid() + "gave response code: " + exaremeResult.getCode() + " and result: " + exaremeResult.getResults());

                experimentDAO.setResult((exaremeResult.getCode() >= 400) ? null : JsonConverters.convertObjectToJsonString(exaremeResult.getResults()));
                experimentDAO.setStatus((exaremeResult.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "There was an exception: " + e.getMessage());

                experimentDAO.setStatus(ExperimentDAO.Status.error);
            }

            finishExperiment(experimentDAO, endpoint);
            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Finished the experiment: " + experimentDAO.toString());
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
        Logging.LogExperimentAction(experimentDTO.getName(), experimentDTO.getUuid(), "Algorithm finished with code: " + code);

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
    public ExperimentDTO runGalaxyWorkflow(ExperimentDTO experimentDTO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();
        Logging.LogUserAction(user.getUsername(), endpoint, "Running a workflow...");

        ExperimentDAO experimentDAO = createExperimentInTheDatabase(experimentDTO, endpoint);
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
                experimentDAO.setStatus(ExperimentDAO.Status.success);

            } else {     // Something unexpected happened
                String msgErr = gson.toJson(response.errorBody());
                Logging.LogUserAction(user.getUsername(), endpoint, "Error Response: " + msgErr);

                // Values are read from streams.
                JSONObject jObjectError = new JSONObject(msgErr);
                String errMsg = jObjectError.get("err_msg").toString();

                experimentDTO.setStatus((response.code() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            }

        } catch (Exception e) {
            Logging.LogUserAction(user.getUsername(), endpoint, "An exception occurred: " + e.getMessage());
            experimentDAO.setStatus(ExperimentDAO.Status.error);
        }
        saveExperiment(experimentDAO, endpoint);

        // Start the process of fetching the status
        updateWorkflowExperiment(experimentDAO, endpoint);

        Logging.LogUserAction(user.getUsername(), endpoint, "Run workflow completed!");

        experimentDTO = experimentDAO.convertToDTO(true);
        return experimentDTO;
    }


    /**
     * This method creates a thread that will fetch the workflow result when it is ready
     *
     * @param experimentDAO will be used to fetch it's workflow status, it should have the workflowHistoryId initialized
     *                      and the result should not already be fetched
     * @return nothing, just updates the experiment
     */
    public void updateWorkflowExperiment(ExperimentDAO experimentDAO, String endpoint) {
        UserDAO user = activeUserService.getActiveUser();

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
                                } else {
                                    experimentDAO.setResult("[" + result + "]");
                                    experimentDAO.setStatus(ExperimentDAO.Status.success);
                                    resultFound = true;
                                }
                            }
                        }

                        if (!resultFound) {      // If there is no visible result
                            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No visible result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }

                        finishExperiment(experimentDAO, endpoint);
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
                                experimentDAO.setStatus(ExperimentDAO.Status.error);
                                failedJobFound = true;
                            }
                        }

                        if (!failedJobFound) {      // If there is no visible failed job
                            Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "No failed result");
                            experimentDAO.setStatus(ExperimentDAO.Status.error);
                        }
                        finishExperiment(experimentDAO, endpoint);
                        break;

                    default:        // InternalError or unexpected result
                        Logging.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "An unexpected error occurred.");
                        experimentDAO.setStatus(ExperimentDAO.Status.error);
                        finishExperiment(experimentDAO, endpoint);
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

        String result;
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

        String state;
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

        List<GalaxyWorkflowResult> getGalaxyWorkflowResultList;
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
     * @param contentId     the id of the job result that we want
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

        String resultJson;
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

        String fullError;
        String returnError;
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
