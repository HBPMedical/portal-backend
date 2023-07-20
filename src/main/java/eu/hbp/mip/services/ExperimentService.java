package eu.hbp.mip.services;

import com.google.gson.Gson;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.models.DTOs.Exareme2AlgorithmRequestDTO;
import eu.hbp.mip.models.DTOs.ExaremeAlgorithmRequestParamDTO;
import eu.hbp.mip.models.DTOs.ExaremeAlgorithmResultDTO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.repositories.ExperimentRepository;
import eu.hbp.mip.repositories.ExperimentSpecifications;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import eu.hbp.mip.utils.Exceptions.NoContent;
import eu.hbp.mip.utils.Exceptions.UnauthorizedException;
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


    private static final Gson gson = new Gson();
    private final ActiveUserService activeUserService;
    private final AlgorithmService algorithmService;
    private final ExperimentRepository experimentRepository;
    @Value("#{'${services.exareme.queryExaremeUrl}'}")
    private String queryExaremeUrl;
    @Value("#{'${services.exareme2.algorithmsUrl}'}")
    private String exareme2AlgorithmsUrl;
    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    public ExperimentService(ActiveUserService activeUserService, AlgorithmService algorithmService, ExperimentRepository experimentRepository) {
        this.algorithmService = algorithmService;
        this.activeUserService = activeUserService;
        this.experimentRepository = experimentRepository;
    }

    /**
     * The getExperiments will retrieve the experiments from database according to the filters.
     *
     * @param name          is optional, in case it is required to filter the experiments by name
     * @param algorithm     is optional, in case it is required to filter the experiments by algorithm name
     * @param shared        is optional, in case it is required to filter the experiments by shared
     * @param viewed        is optional, in case it is required to filter the experiments by viewed
     * @param includeShared is optional, in case it is required to retrieve the experiment that is shared
     * @param page          is the page that is required to be retrieved
     * @param size          is the size of each page
     * @param orderBy       is the column that is required to ordered by
     * @param descending    is a boolean to determine if the experiments will be order by descending or ascending
     * @param logger        contains username and the endpoint.
     * @return a map experiments
     */

    public Map getExperiments(Authentication authentication, String name, String algorithm, Boolean shared, Boolean viewed, boolean includeShared, int page, int size, String orderBy, Boolean descending, Logger logger) {

        UserDAO user = activeUserService.getActiveUser();
        logger.LogUserAction("Listing my experiments.");
        if (size > 50)
            throw new BadRequestException("Invalid size input, max size is 50.");
        Specification<ExperimentDAO> spec;
        if (!authenticationIsEnabled || ClaimUtils.validateAccessRightsOnExperiments(authentication, logger)) {
            spec = Specification
                    .where(new ExperimentSpecifications.ExperimentWithName(name))
                    .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                    .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                    .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                    .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
        } else {
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
     * @param uuid   is the id of the experiment to be retrieved
     * @param logger contains username and the endpoint.
     * @return the experiment information that was retrieved from the database
     */
    public ExperimentDTO getExperiment(Authentication authentication, String uuid, Logger logger) {

        ExperimentDAO experimentDAO;
        UserDAO user = activeUserService.getActiveUser();

        logger.LogUserAction("Loading Experiment with uuid : " + uuid);

        experimentDAO = experimentRepository.loadExperiment(uuid, logger);
        if (
                authenticationIsEnabled
                        && !experimentDAO.isShared()
                        && !experimentDAO.getCreatedBy().getUsername().equals(user.getUsername())
                        && !ClaimUtils.validateAccessRightsOnExperiments(authentication, logger)
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
     * @param logger         contains username and the endpoint.
     * @return the experiment information which was created
     */
    public ExperimentDTO createExperiment(Authentication authentication, ExperimentDTO experimentDTO, Logger logger) {

        // TODO ExperimentRequestDTO should be different than ExperimentResponseDTO
        checkPostExperimentProperInput(experimentDTO, logger);

        // Get the engine name from algorithmService
        String algorithmEngineName = algorithmService.getAlgorithmEngineType(experimentDTO.getAlgorithm().getName().toUpperCase());
        logger.LogUserAction("Algorithm runs on " + algorithmEngineName + ".");

        algorithmParametersLogging(experimentDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, logger);
            ClaimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        return createSynchronousExperiment(experimentDTO, algorithmEngineName, logger);

    }

    /**
     * Will run synchronously an experiment and return the result
     *
     * @param authentication is the role of the user
     * @param experimentDTO  is the experiment information
     * @param logger         contains username and the endpoint.
     * @return the experiment information which was created
     */
    public ExperimentDTO runTransientExperiment(Authentication authentication, ExperimentDTO experimentDTO, Logger logger) {

        //Checking if check (POST) /experiments has proper input.
        checkPostExperimentProperInput(experimentDTO, logger);

        // Get the engine name from algorithmService
        String algorithmEngineName = algorithmService.getAlgorithmEngineType(experimentDTO.getAlgorithm().getName().toUpperCase());


        experimentDTO.setUuid(UUID.randomUUID());

        algorithmParametersLogging(experimentDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getDatasetFromExperimentParameters(experimentDTO, logger);
            ClaimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        logger.LogUserAction("Completed, returning: " + experimentDTO);

        ExaremeAlgorithmResultDTO exaremeAlgorithmResultDTO = runSynchronousExperiment(experimentDTO, algorithmEngineName, logger);

        logger.LogUserAction(
                "Experiment with uuid: " + experimentDTO.getUuid()
                        + "gave response code: " + exaremeAlgorithmResultDTO.getCode()
                        + " and result: " + exaremeAlgorithmResultDTO.getResult()
        );

        experimentDTO.setResult(exaremeAlgorithmResultDTO.getResult());
        experimentDTO.setStatus((exaremeAlgorithmResultDTO.getCode() >= 400)
                ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);

        return experimentDTO;
    }

    /**
     * The updateExperiment will update the experiment's properties
     *
     * @param uuid          is the id of the experiment to be updated
     * @param experimentDTO is the experiment information to be updated
     * @param logger        contains username and the endpoint.
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
            logger.LogUserAction("Attempted to save changes to database but an error occurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }

        logger.LogUserAction("Updated experiment with uuid : " + uuid + ".");

        experimentDTO = new ExperimentDTO(true, experimentDAO);
        return experimentDTO;
    }

    /**
     * The deleteExperiment will delete an experiment from the database
     *
     * @param uuid   is the id of the experiment to be deleted
     * @param logger contains username and the endpoint.
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
            logger.LogUserAction("Attempted to delete an experiment to database but an error occurred  : " + e.getMessage() + ".");
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
            logger.LogUserAction("Invalid input.");
            throw new BadRequestException("Please provide proper input.");
        }
    }

    private void verifyPatchExperimentNonEditableFields(ExperimentDTO experimentDTO, Logger logger) {
        if (experimentDTO.getUuid() != null) {
            logger.LogUserAction("Uuid is not editable.");
            throw new BadRequestException("Uuid is not editable.");
        }

        if (experimentDTO.getAlgorithm() != null) {
            logger.LogUserAction("Algorithm is not editable.");
            throw new BadRequestException("Algorithm is not editable.");
        }

        if (experimentDTO.getCreated() != null) {
            logger.LogUserAction("Created is not editable.");
            throw new BadRequestException("Created is not editable.");
        }

        if (experimentDTO.getCreatedBy() != null) {
            logger.LogUserAction("CreatedBy is not editable.");
            throw new BadRequestException("CreatedBy is not editable.");
        }

        if (experimentDTO.getUpdated() != null) {
            logger.LogUserAction("Updated is not editable.");
            throw new BadRequestException("Updated is not editable.");
        }

        if (experimentDTO.getFinished() != null) {
            logger.LogUserAction("Finished is not editable.");
            throw new BadRequestException("Finished is not editable.");
        }

        if (experimentDTO.getResult() != null) {
            logger.LogUserAction("Result is not editable.");
            throw new BadRequestException("Result is not editable.");
        }

        if (experimentDTO.getStatus() != null) {
            logger.LogUserAction("Status is not editable.");
            throw new BadRequestException("Status is not editable.");
        }
    }

    private void algorithmParametersLogging(ExperimentDTO experimentDTO, Logger logger) {
        String algorithmName = experimentDTO.getAlgorithm().getName();
        StringBuilder parametersLogMessage = new StringBuilder(", Parameters:\n");
        if (experimentDTO.getAlgorithm().getParameters() != null) {
            experimentDTO.getAlgorithm().getParameters().forEach(
                    params -> parametersLogMessage
                            .append("  ")
                            .append(params.getLabel())
                            .append(" -> ")
                            .append(params.getValue())
                            .append("\n"));
        }
        if (experimentDTO.getAlgorithm().getPreprocessing() != null) {
            experimentDTO.getAlgorithm().getPreprocessing().forEach(transformer ->
            {
                parametersLogMessage
                        .append("  ")
                        .append(transformer.getLabel())
                        .append(" -> ")
                        .append("\n");

                if (transformer.getParameters() != null) {
                    transformer.getParameters().forEach(
                            transformerParams -> parametersLogMessage
                                    .append("        ")
                                    .append(transformerParams.getLabel())
                                    .append(" -> ")
                                    .append(transformerParams.getValue())
                                    .append("\n"));
                    parametersLogMessage.append("\n");
                }
            });
        }

        logger.LogUserAction("Executing " + algorithmName + parametersLogMessage);
    }

    /**
     * The getDatasetFromExperimentParameters will retrieve the dataset from the experiment parameters
     *
     * @param experimentDTO is the experiment information
     * @param logger        contains username and the endpoint.
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

    /**
     * Creates an experiment and runs it on the background.
     * Uses the exareme or exareme2 engine that run an experiment synchronously.
     *
     * @param experimentDTO is the request with the experiment information
     * @param logger        contains username and the endpoint.
     * @return the experiment information that was retrieved from exareme
     */
    private ExperimentDTO createSynchronousExperiment(ExperimentDTO experimentDTO, String algorithmEngineName, Logger logger) {

        logger.LogUserAction("Running the algorithm...");

        ExperimentDAO experimentDAO = experimentRepository.createExperimentInTheDatabase(experimentDTO, activeUserService.getActiveUser(), logger);
        experimentDTO.setUuid(experimentDAO.getUuid());
        logger.LogUserAction("Created experiment with uuid :" + experimentDAO.getUuid());

        logger.LogUserAction("Starting execution in thread");
        ExperimentDTO finalExperimentDTO = experimentDTO;
        new Thread(() -> {

            try {
                ExaremeAlgorithmResultDTO exaremeAlgorithmResultDTO = runSynchronousExperiment(finalExperimentDTO, algorithmEngineName, logger);

                logger.LogUserAction(
                        "Experiment with uuid: " + experimentDAO.getUuid()
                                + " gave response code: " + exaremeAlgorithmResultDTO.getCode()
                                + " and result: " + exaremeAlgorithmResultDTO.getResult()
                );

                experimentDAO.setResult(JsonConverters.convertObjectToJsonString(exaremeAlgorithmResultDTO.getResult()));
                experimentDAO.setStatus((exaremeAlgorithmResultDTO.getCode() >= 400)
                        ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                experimentDAO.setStatus(ExperimentDAO.Status.error);
                throw e;
            }

            experimentRepository.finishExperiment(experimentDAO, logger);
            logger.LogUserAction("Finished the experiment: " + experimentDAO);
        }).start();

        experimentDTO = new ExperimentDTO(true, experimentDAO);
        return experimentDTO;
    }

    /**
     * Runs the experiment to exareme or Exareme2, waiting for the response from them.
     * Exareme and Exareme2 do not support async fetching of an algorithm result.
     *
     * @param experimentDTO is the request with the experiment information
     * @return the result of experiment as well as the http status that was retrieved
     */
    private ExaremeAlgorithmResultDTO runSynchronousExperiment(ExperimentDTO experimentDTO, String algorithmEngineName, Logger logger) {
        if (algorithmEngineName.equals("Exareme2")) {
            return runExareme2Experiment(experimentDTO, logger);
        } else {
            return runExaremeExperiment(experimentDTO, logger);
        }
    }

    /**
     * The runExaremeExperiment will run an experiment using Exareme
     *
     * @param experimentDTO contains the information needed to run the experiment
     * @param logger        used to log information
     * @return the result of exareme as well as the http status that was retrieved
     */
    private ExaremeAlgorithmResultDTO runExaremeExperiment(ExperimentDTO experimentDTO, Logger logger) {
        String algorithmName = experimentDTO.getAlgorithm().getName();
        logger.LogUserAction("Starting Exareme algorithm execution, algorithm: " + algorithmName);

        String algorithmEndpoint = queryExaremeUrl + "/" + algorithmName;
        List<ExaremeAlgorithmRequestParamDTO> algorithmParameters
                = experimentDTO.getAlgorithm().getParameters();
        List<ExaremeAlgorithmRequestParamDTO> algorithmParametersWithoutPathologyVersion = new ArrayList<>();

        for (ExaremeAlgorithmRequestParamDTO algorithmParameter : algorithmParameters) {
            if (algorithmParameter.getName().equals("pathology")) {
                List<String> pathology_info = Arrays.asList(algorithmParameter.getValue().split(":", 2));
                String pathology_code = pathology_info.get(0);
                algorithmParameter.setValue(pathology_code);
            }
            algorithmParametersWithoutPathologyVersion.add(algorithmParameter);

        }

        String algorithmBody = gson.toJson(algorithmParametersWithoutPathologyVersion);
        logger.LogUserAction("Exareme algorithm execution. Endpoint: " + algorithmEndpoint);
        logger.LogUserAction("Exareme algorithm execution. Body: " + algorithmBody);

        StringBuilder requestResponseBody = new StringBuilder();
        int requestResponseCode;
        try {
            requestResponseCode = HTTPUtil.sendPost(algorithmEndpoint, algorithmBody, requestResponseBody);
        } catch (Exception e) {
            throw new InternalServerError("Error occurred : " + e.getMessage());
        }

        // Results are stored in the experiment object
        ExaremeAlgorithmResultDTO exaremeResult = JsonConverters.convertJsonStringToObject(
                String.valueOf(requestResponseBody), ExaremeAlgorithmResultDTO.class
        );
        exaremeResult.setCode(requestResponseCode);

        return exaremeResult;
    }

    /**
     * The runExareme2Experiment will run an experiment using the Exareme2
     *
     * @param experimentDTO contains the information needed to run the experiment
     * @param logger        is used to log
     * @return the result of the algorithm
     */
    private ExaremeAlgorithmResultDTO runExareme2Experiment(ExperimentDTO experimentDTO, Logger logger) {
        String algorithmName = experimentDTO.getAlgorithm().getName();
        logger.LogUserAction("Starting Exareme2 algorithm execution, algorithm: " + algorithmName);

        String algorithmEndpoint = exareme2AlgorithmsUrl + "/" + algorithmName.toLowerCase();
        Exareme2AlgorithmRequestDTO exareme2AlgorithmRequestDTO =
                new Exareme2AlgorithmRequestDTO(experimentDTO.getUuid(), experimentDTO.getAlgorithm().getParameters(), experimentDTO.getAlgorithm().getPreprocessing());
        String algorithmBody = JsonConverters.convertObjectToJsonString(exareme2AlgorithmRequestDTO);
        logger.LogUserAction("Exareme2 algorithm execution. Endpoint: " + algorithmEndpoint);
        logger.LogUserAction("Exareme2 algorithm execution. Body: " + algorithmBody);

        StringBuilder requestResponseBody = new StringBuilder();
        int requestResponseCode;
        try {
            requestResponseCode = HTTPUtil.sendPost(algorithmEndpoint, algorithmBody, requestResponseBody);
        } catch (Exception e) {
            logger.LogUserAction("An unexpected error occurred when running a mip experiment: " + e.getMessage());
            throw new InternalServerError("");
        }

        List<Object> exaremeAlgorithmResult = new ArrayList<>();
        if (requestResponseCode == 200) {
            Object exareme2Result =
                    JsonConverters.convertJsonStringToObject(String.valueOf(requestResponseBody), Object.class);
            exaremeAlgorithmResult.add(exareme2Result);

        } else if (requestResponseCode == 400) {
            Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
            exaremeAlgorithmResultElement.put("data", String.valueOf(requestResponseBody));
            exaremeAlgorithmResultElement.put("type", "text/plain+error");
            exaremeAlgorithmResult.add(exaremeAlgorithmResultElement);

        } else if (requestResponseCode == 460) {
            Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
            exaremeAlgorithmResultElement.put("data", String.valueOf(requestResponseBody));
            exaremeAlgorithmResultElement.put("type", "text/plain+user_error");
            exaremeAlgorithmResult.add(exaremeAlgorithmResultElement);

        } else if (requestResponseCode == 461) {
            Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
            exaremeAlgorithmResultElement.put("data", String.valueOf(requestResponseBody));
            exaremeAlgorithmResultElement.put("type", "text/plain+error");
            exaremeAlgorithmResult.add(exaremeAlgorithmResultElement);

        } else if (requestResponseCode == 500) {
            Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
            exaremeAlgorithmResultElement.put("data",
                    "Something went wrong. Please inform the system administrator or try again later."
            );
            exaremeAlgorithmResultElement.put("type", "text/plain+error");
            exaremeAlgorithmResult.add(exaremeAlgorithmResultElement);

        } else if (requestResponseCode == 512) {
            Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
            exaremeAlgorithmResultElement.put("data", String.valueOf(requestResponseBody));
            exaremeAlgorithmResultElement.put("type", "text/plain+error");
            exaremeAlgorithmResult.add(exaremeAlgorithmResultElement);

        } else {
            logger.LogUserAction(
                    "Exareme2 execution responded with an unexpected status code: " + requestResponseCode
            );
            throw new InternalServerError("");
        }

        return new ExaremeAlgorithmResultDTO(requestResponseCode, exaremeAlgorithmResult);

    }
}
