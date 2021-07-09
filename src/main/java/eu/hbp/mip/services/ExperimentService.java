package eu.hbp.mip.services;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.models.DTOs.*;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.services.Specifications.ExperimentSpecifications;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.Exceptions.*;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.JsonConverters;
import eu.hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.*;


@Service
public class ExperimentService {


    @Value("#{'${services.exareme.queryExaremeUrl}'}")
    private String queryExaremeUrl;

    @Value("#{'${services.mipengine.algorithmsUrl}'}")
    private String mipengineAlgorithmsUrl;

    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    private static final Gson gson = new Gson();

    private final ActiveUserService activeUserService;
    private final GalaxyService galaxyService;
    private final ExperimentRepository experimentRepository;

    public ExperimentService(ActiveUserService activeUserService, GalaxyService galaxyService, ExperimentRepository experimentRepository) {
        this.activeUserService = activeUserService;
        this.galaxyService = galaxyService;
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
        if(!authenticationIsEnabled  || ClaimUtils.validateAccessRightsOnExperiments(authentication, logger))
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
        experimentDAOs.forEach(experimentDAO -> experimentDTOs.add(new ExperimentDTO(false, experimentDAO)));

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

        experimentDAO = experimentRepository.loadExperiment(uuid, logger);
        if (
                !experimentDAO.isShared()
                && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername())
                && authenticationIsEnabled
                && ClaimUtils.validateAccessRightsOnExperiments(authentication, logger)
        ) {
            logger.LogUserAction("Accessing Experiment is unauthorized.");
            throw new UnauthorizedException("You don't have access to the experiment.");
        }
        ExperimentDTO experimentDTO = new ExperimentDTO(true, experimentDAO);
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
            ClaimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        // Run with the appropriate engine
        if (algorithmType.equals("workflow")) {
            logger.LogUserAction("Algorithm runs on Galaxy.");
            return galaxyService.runGalaxyWorkflow(experimentDTO, logger);
        } else {
            logger.LogUserAction("Algorithm runs on Exareme.");
            return createExperiment(experimentDTO, logger);
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

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, logger);

        // Get the type and name of algorithm
        String algorithmType = experimentDTO.getAlgorithm().getType();

        if(algorithmType.equals("workflow")){
            logger.LogUserAction("You can not run workflow algorithms transiently.");
            throw new BadRequestException("You can not run workflow algorithms transiently.");
        }

        algorithmParametersLogging(experimentDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, logger);
            ClaimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        logger.LogUserAction("Completed, returning: " + experimentDTO);

        // Results are stored in the experiment object
        ExaremeAlgorithmResultDTO exaremeAlgorithmResultDTO = runExperiment(experimentDTO, logger);

        logger.LogUserAction("Experiment with uuid: " + experimentDTO.getUuid() + "gave response code: " + exaremeAlgorithmResultDTO.getCode() + " and result: " + exaremeAlgorithmResultDTO.getResults());

        experimentDTO.setResult((exaremeAlgorithmResultDTO.getCode() >= 400) ? null : exaremeAlgorithmResultDTO.getResults());
        experimentDTO.setStatus((exaremeAlgorithmResultDTO.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);

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

        experimentDAO = experimentRepository.loadExperiment(uuid, logger);

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

        experimentDTO = new ExperimentDTO(true, experimentDAO);
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

        experimentDAO = experimentRepository.loadExperiment(uuid, logger);

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
        for (ExaremeAlgorithmRequestParamDTO parameter : experimentDTO.getAlgorithm().getParameters()) {
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

    private ExaremeAlgorithmResultDTO convertMIPEngineResultToExaremeAlgorithmResult(int code, String result) {
        MIPEngineAlgorithmResultDTO mipVisualization = JsonConverters.convertJsonStringToObject(result, MIPEngineAlgorithmResultDTO.class);
        LinkedTreeMap<String,Object> data = new LinkedTreeMap<>();
        data.put("data", new TabularVisualizationDTO(mipVisualization));
        data.put("type", "application/vnd.dataresource+json");
        List<Object> finalObject = new ArrayList<>();
        finalObject.add(data);
        return new ExaremeAlgorithmResultDTO(code, finalObject);
    }

    /**
     * The runExperiment will run the experiment to exareme or MIPEngine.
     *
     * @param experimentDTO is the request with the experiment information
     * @return the result of experiment as well as the http status that was retrieved
     */
    public ExaremeAlgorithmResultDTO runExperiment(ExperimentDTO experimentDTO, Logger logger) {

        // Algorithm type
        String algorithmType = experimentDTO.getAlgorithm().getType();

        // Run the 1st algorithm from the list
        String algorithmName = experimentDTO.getAlgorithm().getName();

        // Run with the appropriate engine
        if (algorithmType.equals("mipengine")) {
            MIPEngineAlgorithmRequestDTO mipEngineAlgorithmRequestDTO = new MIPEngineAlgorithmRequestDTO(experimentDTO.getAlgorithm().getParameters());
            String body = JsonConverters.convertObjectToJsonString(mipEngineAlgorithmRequestDTO);
            String url =  mipengineAlgorithmsUrl + "/" + algorithmName.toLowerCase();
            logger.LogUserAction("url: " + url + ", body: " + body);
            logger.LogUserAction("Algorithm runs on MIPEngine.");
            return runMIPEngineExperiment(url, body);
        } else {
            List<ExaremeAlgorithmRequestParamDTO> algorithmParameters
                    = experimentDTO.getAlgorithm().getParameters();
            String body = gson.toJson(algorithmParameters);
            String url = queryExaremeUrl + "/" + algorithmName;
            logger.LogUserAction("url: " + url + ", body: " + body);
            logger.LogUserAction("Algorithm runs on Exareme.");
            return runExaremeExperiment(url, body);
        }
    }


    /**
     * The runExaremeExperiment will run to exareme the experiment
     *
     * @param url           is the url that contain the results of the experiment
     * @param body          is the parameters of the algorithm
     * @return the result of exareme as well as the http status that was retrieved
     */
    public ExaremeAlgorithmResultDTO runExaremeExperiment(String url, String body) {

        StringBuilder results = new StringBuilder();
        int code;
        try {
            code = HTTPUtil.sendPost(url, body, results);
        } catch (Exception e) {
            throw new InternalServerError("Error occurred : " + e.getMessage());
        }

        // Results are stored in the experiment object
        ExaremeAlgorithmResultDTO exaremeResult = JsonConverters.convertJsonStringToObject(
                String.valueOf(results), ExaremeAlgorithmResultDTO.class
        );
        exaremeResult.setCode(code);

        return exaremeResult;
    }

    /**
     * The runExaremeExperiment will run to exareme the experiment
     *
     * @param url           is the url that contain the results of the experiment
     * @param body          is the parameters of the algorithm
     * @return the result of exareme as well as the http status that was retrieved
     */
    public ExaremeAlgorithmResultDTO runMIPEngineExperiment(String url, String body) {

        StringBuilder results = new StringBuilder();
        int code;
        try {
            code = HTTPUtil.sendPost(url, body, results);
        } catch (Exception e) {
            throw new InternalServerError("Error occurred : " + e.getMessage());
        }
        System.out.println(results);
        // Results are stored in the experiment object
        return convertMIPEngineResultToExaremeAlgorithmResult(code, String.valueOf(results));
    }

    /* --------------------------------------  EXAREME CALLS ---------------------------------------------------------*/

    /**
     * The createExaremeExperiment will POST the algorithm to the exareme client
     *
     * @param experimentDTO is the request with the experiment information
     * @param logger    contains username and the endpoint.
     * @return the experiment information that was retrieved from exareme
     */
    public ExperimentDTO createExperiment(ExperimentDTO experimentDTO, Logger logger) {

        logger.LogUserAction("Running the algorithm...");

        ExperimentDAO experimentDAO = experimentRepository.createExperimentInTheDatabase(experimentDTO, activeUserService.getActiveUser(), logger);
        logger.LogUserAction("Created experiment with uuid :" + experimentDAO.getUuid());

        logger.LogUserAction("Starting execution in thread");
        ExperimentDTO finalExperimentDTO = experimentDTO;
        new Thread(() -> {

            // ATTENTION: Inside the Thread only LogExperimentAction should be used, not LogUserAction!
            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Thread named :" + Thread.currentThread().getName() + " with id :" + Thread.currentThread().getId() + " started!");

            try {

                // Results are stored in the experiment object
                ExaremeAlgorithmResultDTO exaremeAlgorithmResultDTO = runExperiment(finalExperimentDTO, logger);

                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Experiment with uuid: " + experimentDAO.getUuid() + "gave response code: " + exaremeAlgorithmResultDTO.getCode() + " and result: " + exaremeAlgorithmResultDTO.getResults());

                experimentDAO.setResult((exaremeAlgorithmResultDTO.getCode() >= 400) ? null : JsonConverters.convertObjectToJsonString(exaremeAlgorithmResultDTO.getResults()));
                experimentDAO.setStatus((exaremeAlgorithmResultDTO.getCode() >= 400) ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "There was an exception: " + e.getMessage());

                experimentDAO.setStatus(ExperimentDAO.Status.error);
            }

            experimentRepository.finishExperiment(experimentDAO, logger);
            Logger.LogExperimentAction(experimentDAO.getName(), experimentDAO.getUuid(), "Finished the experiment: " + experimentDAO);
        }).start();
        experimentDTO = new ExperimentDTO(true, experimentDAO);
        return experimentDTO;
    }

    /* ---------------------------------------  GALAXY CALLS ---------------------------------------------------------*/

}
