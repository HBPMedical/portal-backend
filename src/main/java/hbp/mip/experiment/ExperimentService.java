package hbp.mip.experiment;

import hbp.mip.algorithm.Exareme2AlgorithmsSpecs;
import hbp.mip.algorithm.Exareme2AlgorithmRequestDTO;
import hbp.mip.algorithm.Exareme2AlgorithmSpecificationDTO;
import hbp.mip.user.ActiveUserService;
import hbp.mip.user.UserDTO;
import hbp.mip.utils.*;
import hbp.mip.utils.Exceptions.BadRequestException;
import hbp.mip.utils.Exceptions.InternalServerError;
import hbp.mip.utils.Exceptions.NoContent;
import hbp.mip.utils.Exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static hbp.mip.utils.JsonConverters.convertObjectToJsonString;


@Service
public class ExperimentService {

    private final ActiveUserService activeUserService;

    private final ClaimUtils claimUtils;
    private final Exareme2AlgorithmsSpecs exareme2AlgorithmsSpecs;

    private final ExperimentRepository experimentRepository;

    @Value("${services.exareme2.algorithmsUrl}")
    private String exareme2AlgorithmsUrl;

    @Value("${authentication.enabled}")
    private boolean authenticationIsEnabled;

    public ExperimentService(
            ActiveUserService activeUserService,
            ClaimUtils claimUtils,
            Exareme2AlgorithmsSpecs exareme2AlgorithmsSpecs,
            ExperimentRepository experimentRepository
    ) {
        this.activeUserService = activeUserService;
        this.claimUtils = claimUtils;
        this.exareme2AlgorithmsSpecs = exareme2AlgorithmsSpecs;
        this.experimentRepository = experimentRepository;
    }

    private static List<Object> convertExareme2ResponseToAlgorithmResults(Logger logger, int requestResponseCode, StringBuilder requestResponseBody) {
        Object result;
        if (requestResponseCode == 200) {
            result = JsonConverters.convertJsonStringToObject(String.valueOf(requestResponseBody), Object.class);
        } else if (requestResponseCode == 400) {
            result = convertExareme2ResponseToAlgorithmResult(String.valueOf(requestResponseBody), "text/plain+error");
        } else if (requestResponseCode == 460) {
            result = convertExareme2ResponseToAlgorithmResult(String.valueOf(requestResponseBody), "text/plain+user_error");
        } else if (requestResponseCode == 461) {
            result = convertExareme2ResponseToAlgorithmResult(String.valueOf(requestResponseBody), "text/plain+error");
        } else if (requestResponseCode == 500) {
            result = convertExareme2ResponseToAlgorithmResult("Something went wrong. Please inform the system administrator or try again later.", "text/plain+error");
        } else if (requestResponseCode == 512) {
            result = convertExareme2ResponseToAlgorithmResult(String.valueOf(requestResponseBody), "text/plain+error");
        } else {
            var errorMessage = "Exareme2 execution responded with an unexpected status code: " + requestResponseCode;
            logger.error(errorMessage);
            throw new InternalServerError(errorMessage);
        }
        return List.of(result);
    }

    private static Map<String, Object> convertExareme2ResponseToAlgorithmResult(String resultBody, String resultType) {
        Map<String, Object> exaremeAlgorithmResultElement = new HashMap<>();
        exaremeAlgorithmResultElement.put("data", resultBody);
        exaremeAlgorithmResultElement.put("type", resultType);
        return exaremeAlgorithmResultElement;
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
     * @param descending    is a boolean to determine if the experiments will be ordered by descending or ascending order
     * @param logger        contains username and the endpoint.
     * @return a map experiments
     */
    public ExperimentsDTO getExperiments(Authentication authentication, String name, String algorithm, Boolean shared, Boolean viewed, boolean includeShared, int page, int size, String orderBy, Boolean descending, Logger logger) {
        var user = activeUserService.getActiveUser(authentication);
        if (size > 50)
            throw new BadRequestException("Invalid size input, max size is 50.");

        Specification<ExperimentDAO> spec;
        if (!authenticationIsEnabled || claimUtils.validateAccessRightsOnALLExperiments(authentication, logger)) {
            spec = Specification
                    .where(new ExperimentSpecifications.ExperimentWithName(name))
                    .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                    .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                    .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                    .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
        } else {
            spec = Specification
                    .where(new ExperimentSpecifications.MyExperiment(user.username()))
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

        List<ExperimentDTO> experiments = new ArrayList<>();
        experimentDAOs.forEach(experimentDAO -> experiments.add(new ExperimentDTO(experimentDAO, false)));

        return new ExperimentsDTO(
                experiments,
                pageExperiments.getNumber(),
                pageExperiments.getTotalPages(),
                pageExperiments.getTotalElements()
        );
    }

    public ExperimentDTO getExperiment(Authentication authentication, String uuid, Logger logger) {
        var user = activeUserService.getActiveUser(authentication);

        var experimentDAO = experimentRepository.loadExperiment(uuid, logger);
        if (
                authenticationIsEnabled
                        && !experimentDAO.isShared()
                        && !experimentDAO.getCreatedBy().getUsername().equals(user.username())
                        && !claimUtils.validateAccessRightsOnALLExperiments(authentication, logger)
        ) {
            logger.warn("User tried to access an unauthorized experiment with id:" + uuid);
            throw new UnauthorizedException("You don't have access to that experiment.");
        }

        return new ExperimentDTO(experimentDAO, true);
    }

    public ExperimentDTO createExperiment(Authentication authentication, ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        algorithmParametersLogging(experimentExecutionDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getExperimentDatasets(experimentExecutionDTO, logger);
            claimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        var experimentDAO = experimentRepository.createExperimentInTheDatabase(experimentExecutionDTO, activeUserService.getActiveUser(authentication), logger);

        new Thread(() -> {
            logger.debug("Experiment's algorithm execution started in a background thread.");
            try {
                ExperimentAlgorithmResultDTO exaremeExperimentAlgorithmResultDTO = runExaremeAlgorithm(experimentDAO.getUuid(), experimentExecutionDTO, logger);
                experimentDAO.setResult(convertObjectToJsonString(exaremeExperimentAlgorithmResultDTO.result()));
                experimentDAO.setStatus((exaremeExperimentAlgorithmResultDTO.code() >= 400)
                        ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                logger.error("Exareme2 algorithm execution failed: " + e.getMessage() + "\nStacktrace: " + Arrays.toString(e.getStackTrace()));
                experimentDAO.setStatus(ExperimentDAO.Status.error);
            }

            experimentRepository.finishExperiment(experimentDAO, logger);

            // Experiment finished log is needed for the federation info (mip-deployment) script.
            logger.info("Experiment finished: " + experimentDAO);
        }).start();

        return new ExperimentDTO(experimentDAO, false);
    }

    public ExperimentDTO runTransientExperiment(Authentication authentication, ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        algorithmParametersLogging(experimentExecutionDTO, logger);

        if (authenticationIsEnabled) {
            String experimentDatasets = getExperimentDatasets(experimentExecutionDTO, logger);
            claimUtils.validateAccessRightsOnDatasets(authentication, experimentDatasets, logger);
        }

        var uuid = UUID.randomUUID();
        var algorithmResult = runExaremeAlgorithm(uuid, experimentExecutionDTO, logger);

        return new ExperimentDTO(
                uuid,
                experimentExecutionDTO.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                algorithmResult.result(),
                algorithmResult.code() >= 400 ? ExperimentDAO.Status.error : ExperimentDAO.Status.success,
                experimentExecutionDTO.algorithm()
        );
    }

    public ExperimentDTO updateExperiment(UserDTO user, String uuid, ExperimentDTO experiment, Logger logger) {
        var experimentDAO = experimentRepository.loadExperiment(uuid, logger);

        verifyNonEditableFieldsAreNotBeingModified(experiment, logger);

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.username())) {
            logger.warn("User tried to modify a unauthorized experiment with uuid: " + uuid);
            throw new UnauthorizedException("You don't have access to modify the experiment.");
        }

        // Change modifiable fields if provided
        if (experiment.name() != null && !experiment.name().isEmpty())
            experimentDAO.setName(experiment.name());

        if (experiment.shared() != null)
            experimentDAO.setShared(experiment.shared());

        if (experiment.viewed() != null)
            experimentDAO.setViewed(experiment.viewed());

        experimentDAO.setUpdated(new Date());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.error("Failed to save to the database: " + e.getMessage());
            throw e;
        }

        return new ExperimentDTO(experimentDAO, true);
    }

    //    /* -------------------------------  PRIVATE METHODS  ----------------------------------------------------*/

    public void deleteExperiment(UserDTO user, String uuid, Logger logger) {
        ExperimentDAO experimentDAO = experimentRepository.loadExperiment(uuid, logger);

        if (!experimentDAO.getCreatedBy().getUsername().equals(user.username())) {
            logger.warn("User " + user.username() + " tried to delete the experiment with uuid " + uuid + " but he was unauthorized.");
            throw new UnauthorizedException("You don't have access to delete the experiment.");
        }

        try {
            experimentRepository.delete(experimentDAO);
        } catch (Exception e) {
            logger.info("Attempted to delete an experiment from the database but an error occurred: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }
    }

    private void verifyNonEditableFieldsAreNotBeingModified(ExperimentDTO experimentDTO, Logger logger) {
        throwNonEditableExceptionIfNotNull(experimentDTO.uuid(), "uuid", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.algorithm(), "algorithm", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.created(), "created", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.updated(), "updated", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.finished(), "finished", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.createdBy(), "createdBy", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.result(), "result", logger);
        throwNonEditableExceptionIfNotNull(experimentDTO.status(), "status", logger);
    }

    private void throwNonEditableExceptionIfNotNull(Object field, String nonEditableField, Logger logger) {
        if (field != null) {
            var errorMessage = "Tried to edit non editable field: " + nonEditableField;
            logger.warn(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    private void algorithmParametersLogging(ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm = experimentExecutionDTO.algorithm();

        String algorithmName = algorithm.name();

        StringBuilder parametersLogMessage = new StringBuilder();
        if (algorithm.parameters() != null) {
            algorithm.parameters().forEach(
                    params -> parametersLogMessage
                            .append("  ")
                            .append(params.name())
                            .append(" -> ")
                            .append(params.value())
                            .append("\n"));
        }

        if (algorithm.preprocessing() != null) {
            algorithm.preprocessing().forEach(transformer ->
            {
                parametersLogMessage
                        .append("  ")
                        .append(transformer.name())
                        .append(" -> ")
                        .append("\n");

                if (transformer.parameters() != null) {
                    transformer.parameters().forEach(
                            transformerParams -> parametersLogMessage
                                    .append("        ")
                                    .append(transformerParams.name())
                                    .append(" -> ")
                                    .append(transformerParams.value())
                                    .append("\n"));
                    parametersLogMessage.append("\n");
                }
            });
        }

        logger.debug("Algorithm " + algorithmName + " execution starting with parameters: \n" + parametersLogMessage);
    }

    private String getExperimentDatasets(ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        String experimentDatasets = null;
        for (ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO parameter : experimentExecutionDTO.algorithm().parameters()) {
            if (parameter.name().equals("dataset")) {
                experimentDatasets = parameter.value();
                break;
            }
        }

        if (experimentDatasets == null || experimentDatasets.isEmpty()) {
            logger.debug("No datasets provided in experiment execution request.");
            throw new BadRequestException("Please provide at least one dataset.");
        }
        return experimentDatasets;
    }

    private ExperimentAlgorithmResultDTO runExaremeAlgorithm(UUID uuid, ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        String algorithmName = experimentExecutionDTO.algorithm().name();
        String algorithmEndpoint = exareme2AlgorithmsUrl + "/" + algorithmName;
        Exareme2AlgorithmSpecificationDTO exareme2AlgorithmSpecificationDTO = getAlgorithmSpec(algorithmName);
        var exareme2AlgorithmRequestDTO = new Exareme2AlgorithmRequestDTO(uuid, experimentExecutionDTO.algorithm().parameters(), experimentExecutionDTO.algorithm().preprocessing(), exareme2AlgorithmSpecificationDTO);
        String algorithmBody = convertObjectToJsonString(exareme2AlgorithmRequestDTO);
        logger.debug("Exareme2 algorithm request, endpoint: " + algorithmEndpoint);
        logger.debug("Exareme2 algorithm request, body: " + algorithmBody);

        int requestResponseCode;
        var requestResponseBody = new StringBuilder();
        try {
            requestResponseCode = HTTPUtil.sendPost(algorithmEndpoint, algorithmBody, requestResponseBody);
        } catch (IOException e) {
            var errorMessage = "Could not run the exareme2 algorithm: " + e.getMessage();
            logger.error(errorMessage);
            throw new InternalServerError(errorMessage);
        }

        var result = convertExareme2ResponseToAlgorithmResults(logger, requestResponseCode, requestResponseBody);
        return new ExperimentAlgorithmResultDTO(requestResponseCode, result);
    }

    private Exareme2AlgorithmSpecificationDTO getAlgorithmSpec(String algorithmName){
        Optional<Exareme2AlgorithmSpecificationDTO> algorithmSpecification = exareme2AlgorithmsSpecs.getAlgorithms().stream()
                .filter(algorithmSpec-> algorithmSpec.name().equals(algorithmName))
                .findFirst();
        if (algorithmSpecification.isEmpty()) throw new InternalServerError("Missing the algorithm: " + algorithmName);
        return algorithmSpecification.get();
    }

    record ExperimentAlgorithmResultDTO(int code, List<Object> result) {
    }
}
