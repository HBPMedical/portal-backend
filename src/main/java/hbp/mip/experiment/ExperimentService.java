package hbp.mip.experiment;

import hbp.mip.algorithm.AlgorithmRequestDTO;
import hbp.mip.user.ActiveUserService;
import hbp.mip.user.UserDTO;
import hbp.mip.utils.*;
import hbp.mip.utils.Exceptions.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static hbp.mip.utils.JsonConverters.convertObjectToJsonString;

@Service
public class ExperimentService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String GENERIC_ERROR_MESSAGE = "Something went wrong. Please inform the system administrator or try again later.";

    private final ActiveUserService activeUserService;
    private final ClaimUtils claimUtils;
    private final ExperimentRepository experimentRepository;

    @Value("${services.exaflow.algorithmsUrl}")
    private String exaflowAlgorithmsUrl;

    @Value("${authentication.enabled}")
    private boolean authenticationIsEnabled;

    public ExperimentService(ActiveUserService activeUserService, ClaimUtils claimUtils,
            ExperimentRepository experimentRepository) {
        this.activeUserService = activeUserService;
        this.claimUtils = claimUtils;
        this.experimentRepository = experimentRepository;
    }

    private static Object convertResponseToAlgorithmResults(Logger logger, int code, StringBuilder responseBody) {
        return switch (code) {
            case 200 -> JsonConverters.convertJsonStringToObject(responseBody.toString(), Object.class);
            case 400, 460, 461, 512 -> convertExaflowResponseToAlgorithmResult(responseBody.toString());
            case 500 -> convertExaflowResponseToAlgorithmResult(GENERIC_ERROR_MESSAGE);
            default -> handleUnexpectedResponseCode(logger, code);
        };
    }

    private static Map<String, Object> convertExaflowResponseToAlgorithmResult(String resultBody) {
        return Map.of("data", resultBody, "type", "text/plain+error");
    }

    private static Object handleUnexpectedResponseCode(Logger logger, int code) {
        String errorMessage = "Exaflow execution responded with an unexpected status code: " + code;
        logger.error(errorMessage);
        throw new InternalServerError(errorMessage);
    }

    /**
     * The getExperiments will retrieve the experiments from database according to
     * the filters.
     *
     * @param name          is optional, in case it is required to filter the
     *                      experiments by name
     * @param algorithm     is optional, in case it is required to filter the
     *                      experiments by algorithm name
     * @param shared        is optional, in case it is required to filter the
     *                      experiments by shared
     * @param viewed        is optional, in case it is required to filter the
     *                      experiments by viewed
     * @param includeShared is optional, in case it is required to retrieve the
     *                      experiment that is shared
     * @param page          is the page that is required to be retrieved
     * @param size          is the size of each page
     * @param orderBy       is the column that is required to ordered by
     * @param descending    is a boolean to determine if the experiments will be
     *                      ordered by descending or ascending order
     * @param logger        contains username and the endpoint.
     * @return a map experiments
     */
    public ExperimentsDTO getExperiments(Authentication authentication, String name, String algorithm, Boolean shared,
            Boolean viewed, boolean includeShared, boolean mine, int page, int size, String orderBy, Boolean descending,
            Logger logger) {
        validatePageSize(size);

        Specification<ExperimentDAO> spec = buildExperimentSpecification(authentication, name, algorithm, shared,
                viewed, includeShared, mine, orderBy, descending, logger);
        Pageable pageable = PageRequest.of(page, size);
        Page<ExperimentDAO> experimentsPage = experimentRepository.findAll(spec, pageable);

        if (experimentsPage.isEmpty()) {
            throw new NoContent("No experiment found with the filters provided.");
        }

        return createExperimentsDTO(experimentsPage);
    }

    private void validatePageSize(int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Invalid size input, max size is " + MAX_PAGE_SIZE);
        }
    }

    private Specification<ExperimentDAO> buildExperimentSpecification(Authentication authentication, String name,
            String algorithm, Boolean shared, Boolean viewed, boolean includeShared, boolean mine, String orderBy,
            Boolean descending, Logger logger) {
        var user = activeUserService.getActiveUser(authentication);
        boolean hasAccessRights = !authenticationIsEnabled
                || claimUtils.validateAccessRightsOnALLExperiments(authentication, logger);

        Specification<ExperimentDAO> spec;

        if (mine) {
            spec = Specification.where(new ExperimentSpecifications.MyExperiment(user.username()));
        } else if (hasAccessRights) {
            spec = Specification.where(null);
        } else {
            spec = Specification.where(new ExperimentSpecifications.MyExperiment(user.username()))
                    .or(new ExperimentSpecifications.SharedExperiment(includeShared));
        }

        return spec
                .and(new ExperimentSpecifications.ExperimentWithName(name))
                .and(new ExperimentSpecifications.ExperimentWithAlgorithm(algorithm))
                .and(new ExperimentSpecifications.ExperimentWithShared(shared))
                .and(new ExperimentSpecifications.ExperimentWithViewed(viewed))
                .and(new ExperimentSpecifications.ExperimentOrderBy(orderBy, descending));
    }

    private ExperimentsDTO createExperimentsDTO(Page<ExperimentDAO> pageExperiments) {
        List<ExperimentDTO> experiments = pageExperiments.map(experimentDAO -> new ExperimentDTO(experimentDAO, false))
                .getContent();
        return new ExperimentsDTO(experiments, pageExperiments.getNumber(), pageExperiments.getTotalPages(),
                pageExperiments.getTotalElements());
    }

    public ExperimentDTO getExperiment(Authentication authentication, String uuid, Logger logger) {
        ExperimentDAO experimentDAO = experimentRepository.loadExperiment(uuid, logger);
        validateExperimentAccess(authentication, experimentDAO, uuid, logger);

        return new ExperimentDTO(experimentDAO, true);
    }

    private void validateExperimentAccess(Authentication authentication, ExperimentDAO experimentDAO, String uuid,
            Logger logger) {
        var user = activeUserService.getActiveUser(authentication);
        boolean unauthorizedAccess = authenticationIsEnabled && !experimentDAO.isShared()
                && !experimentDAO.getCreatedBy().getUsername().equals(user.username())
                && !claimUtils.validateAccessRightsOnALLExperiments(authentication, logger);

        if (unauthorizedAccess) {
            logger.warn("User tried to access an unauthorized experiment with id:" + uuid);
            throw new UnauthorizedException("You don't have access to that experiment.");
        }
    }

    public ExperimentDTO createExperiment(Authentication authentication, ExperimentExecutionDTO experimentExecutionDTO,
            Logger logger) {
        algorithmParametersLogging(experimentExecutionDTO, logger);

        validateDatasetAccess(authentication, experimentExecutionDTO, logger);

        ExperimentDAO experimentDAO = experimentRepository.createExperimentInTheDatabase(experimentExecutionDTO,
                activeUserService.getActiveUser(authentication), logger);
        runAlgorithmInBackground(experimentDAO, experimentExecutionDTO, logger);

        return new ExperimentDTO(experimentDAO, false);
    }

    private void validateDatasetAccess(Authentication authentication, ExperimentExecutionDTO experimentExecutionDTO,
            Logger logger) {
        if (authenticationIsEnabled) {
            claimUtils.validateAccessRightsOnDatasets(authentication,
                    experimentExecutionDTO.algorithm().inputdata().datasets(), logger);
        }
    }

    private void runAlgorithmInBackground(ExperimentDAO experimentDAO, ExperimentExecutionDTO experimentExecutionDTO,
            Logger logger) {
        new Thread(() -> {
            try {
                logger.debug("Experiment's algorithm execution started in a background thread.");
                ExperimentAlgorithmResultDTO resultDTO = runExaflowAlgorithm(experimentDAO.getUuid(),
                        experimentExecutionDTO, logger);
                experimentDAO.setResult(convertObjectToJsonString(resultDTO.result()));
                experimentDAO
                        .setStatus(resultDTO.code() >= 400 ? ExperimentDAO.Status.error : ExperimentDAO.Status.success);
            } catch (Exception e) {
                logger.error("Exaflow algorithm execution failed: " + e.getMessage());
                experimentDAO.setStatus(ExperimentDAO.Status.error);
            }
            experimentRepository.finishExperiment(experimentDAO, logger);
            logger.info("Experiment finished: " + experimentDAO);
        }).start();
    }

    public ExperimentDTO runTransientExperiment(Authentication authentication,
            ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        algorithmParametersLogging(experimentExecutionDTO, logger);

        validateDatasetAccess(authentication, experimentExecutionDTO, logger);
        UUID uuid = UUID.randomUUID();
        ExperimentAlgorithmResultDTO algorithmResult = runExaflowAlgorithm(uuid, experimentExecutionDTO, logger);

        return new ExperimentDTO(uuid, experimentExecutionDTO.name(), null, null, null, null, null, null,
                algorithmResult.result(),
                algorithmResult.code() >= 400 ? ExperimentDAO.Status.error : ExperimentDAO.Status.success,
                experimentExecutionDTO.algorithm(),
                experimentExecutionDTO.mipVersion());
    }

    public ExperimentDTO updateExperiment(UserDTO user, String uuid, ExperimentDTO experiment, Logger logger) {
        ExperimentDAO experimentDAO = experimentRepository.loadExperiment(uuid, logger);

        verifyNonEditableFieldsAreNotBeingModified(experiment, logger);
        checkUpdateAuthorization(user, experimentDAO, uuid, logger);

        updateModifiableFields(experiment, experimentDAO);
        experimentDAO.setUpdated(new Date());

        try {
            experimentRepository.save(experimentDAO);
        } catch (Exception e) {
            logger.error("Failed to save to the database: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }

        return new ExperimentDTO(experimentDAO, true);
    }

    private void checkUpdateAuthorization(UserDTO user, ExperimentDAO experimentDAO, String uuid, Logger logger) {
        if (!experimentDAO.getCreatedBy().getUsername().equals(user.username())) {
            logger.warn("User tried to modify an unauthorized experiment with uuid: " + uuid);
            throw new UnauthorizedException("You don't have access to modify the experiment.");
        }
    }

    private void updateModifiableFields(ExperimentDTO experiment, ExperimentDAO experimentDAO) {
        if (experiment.name() != null && !experiment.name().isEmpty()) {
            experimentDAO.setName(experiment.name());
        }
        if (experiment.shared() != null) {
            experimentDAO.setShared(experiment.shared());
        }
        if (experiment.viewed() != null) {
            experimentDAO.setViewed(experiment.viewed());
        }
    }

    public void deleteExperiment(UserDTO user, String uuid, Logger logger) {
        ExperimentDAO experimentDAO = experimentRepository.loadExperiment(uuid, logger);

        checkDeleteAuthorization(user, experimentDAO, uuid, logger);

        try {
            experimentRepository.delete(experimentDAO);
        } catch (Exception e) {
            logger.info("Attempted to delete an experiment from the database but an error occurred: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }
    }

    private void checkDeleteAuthorization(UserDTO user, ExperimentDAO experimentDAO, String uuid, Logger logger) {
        if (!experimentDAO.getCreatedBy().getUsername().equals(user.username())) {
            logger.warn("User " + user.username() + " tried to delete the experiment with uuid " + uuid
                    + " but was unauthorized.");
            throw new UnauthorizedException("You don't have access to delete the experiment.");
        }
    }

    private void verifyNonEditableFieldsAreNotBeingModified(ExperimentDTO experimentDTO, Logger logger) {
        List.of("uuid", "algorithm", "created", "updated", "finished", "createdBy", "result", "status")
                .forEach(field -> throwNonEditableExceptionIfNotNull(getFieldValue(experimentDTO, field), field,
                        logger));
    }

    private void throwNonEditableExceptionIfNotNull(Object field, String nonEditableField, Logger logger) {
        if (field != null) {
            String errorMessage = "Tried to edit non-editable field: " + nonEditableField;
            logger.warn(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    private Object getFieldValue(ExperimentDTO experimentDTO, String fieldName) {
        try {
            var field = ExperimentDTO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(experimentDTO);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private void algorithmParametersLogging(ExperimentExecutionDTO experimentExecutionDTO, Logger logger) {
        ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm = experimentExecutionDTO.algorithm();
        StringBuilder parametersLogMessage = new StringBuilder();

        Optional.ofNullable(algorithm.parameters()).ifPresent(parameters -> parameters.forEach((paramName,
                paramValue) -> parametersLogMessage.append(" ").append(paramName).append(" -> ").append(paramValue)));

        Optional.ofNullable(algorithm.preprocessing()).ifPresent(preprocessing -> preprocessing
                .forEach((name, value) -> parametersLogMessage.append(" ").append(name).append(" -> ").append(value)));

        if (algorithm.inputdata() != null) {
            AlgorithmRequestDTO.InputDataRequestDTO inputData = algorithm.inputdata();
            parametersLogMessage.append(" Input Data Model: ").append(inputData.data_model());

            Optional.ofNullable(inputData.datasets())
                    .ifPresent(datasets -> parametersLogMessage.append(" Datasets: ").append(datasets));

            Optional.ofNullable(inputData.x())
                    .ifPresent(xVars -> parametersLogMessage.append(" X Variables: ").append(xVars));

            Optional.ofNullable(inputData.y())
                    .ifPresent(yVars -> parametersLogMessage.append(" Y Variables: ").append(yVars));

            if (inputData.filters() != null) {
                AlgorithmRequestDTO.FilterRequestDTO filters = inputData.filters();
                parametersLogMessage.append(" Filter Condition: ").append(filters.condition());

                Optional.ofNullable(filters.rules())
                        .ifPresent(rules -> parametersLogMessage.append(" Filter Rules: ").append(rules));
            }
        }

        logger.debug("Algorithm " + algorithm.name() + " execution starting with parameters: " + parametersLogMessage);
    }

    private ExperimentAlgorithmResultDTO runExaflowAlgorithm(UUID uuid, ExperimentExecutionDTO experimentExecutionDTO,
            Logger logger) {
        String algorithmEndpoint = exaflowAlgorithmsUrl + "/" + experimentExecutionDTO.algorithm().name();
        var requestBody = convertObjectToJsonString(
                AlgorithmRequestDTO.create(uuid, experimentExecutionDTO.algorithm()));

        logger.debug("Exaflow algorithm request, endpoint: " + algorithmEndpoint);
        logger.debug("Exaflow algorithm request, body: " + requestBody);

        int responseCode;
        var responseBody = new StringBuilder();
        try {
            responseCode = HTTPUtil.sendPost(algorithmEndpoint, requestBody, responseBody);
        } catch (IOException e) {
            logger.error("Could not run the exaflow algorithm: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }

        Object result = convertResponseToAlgorithmResults(logger, responseCode, responseBody);
        return new ExperimentAlgorithmResultDTO(responseCode, result);
    }

    record ExperimentAlgorithmResultDTO(int code, Object result) {
    }
}
