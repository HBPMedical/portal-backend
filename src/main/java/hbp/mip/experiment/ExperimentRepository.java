package hbp.mip.experiment;

import hbp.mip.user.UserDAO;
import hbp.mip.user.UserDTO;
import hbp.mip.utils.Exceptions.BadRequestException;
import hbp.mip.utils.Exceptions.ExperimentNotFoundException;
import hbp.mip.utils.JsonConverters;
import hbp.mip.utils.Logger;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Date;
import java.util.UUID;

@RestResource(exported = false)
public interface ExperimentRepository extends CrudRepository<ExperimentDAO, UUID>, JpaSpecificationExecutor<ExperimentDAO> {
    ExperimentDAO findByUuid(UUID experimentUuid);

    default ExperimentDAO loadExperiment(String uuid, Logger logger) {
        UUID experimentUuid;

        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            logger.error("Conversion of string to UUID failed:" +  e.getMessage());
            throw new BadRequestException(e.getMessage());
        }

        ExperimentDAO experimentDAO = findByUuid(experimentUuid);
        if (experimentDAO == null) {
            var errorMessage = "Experiment with uuid : " + uuid + "was not found.";
            logger.warn(errorMessage);
            throw new ExperimentNotFoundException(errorMessage);
        }

        return experimentDAO;
    }

    default ExperimentDAO createExperimentInTheDatabase(ExperimentExecutionDTO experimentExecutionDTO, UserDTO user, Logger logger) {
        ExperimentDAO experimentDAO = new ExperimentDAO();
        experimentDAO.setUuid(UUID.randomUUID());
        experimentDAO.setCreatedBy(new UserDAO(user));
        experimentDAO.setAlgorithm(JsonConverters.convertObjectToJsonString(experimentExecutionDTO.algorithm()));
        experimentDAO.setAlgorithmId(experimentExecutionDTO.algorithm().name());
        experimentDAO.setName(experimentExecutionDTO.name());
        experimentDAO.setStatus(ExperimentDAO.Status.pending);

        try {
            save(experimentDAO);
        } catch (Exception e) {
            logger.error("Failed to save to the database: " + e.getMessage());
            throw e;
        }
        return experimentDAO;
    }

    default void finishExperiment(ExperimentDAO experimentDAO, Logger logger) {
        experimentDAO.setFinished(new Date());

        try {
            save(experimentDAO);
        } catch (Exception e) {
            logger.error("Failed to save to the database: " + e.getMessage());
            throw e;
        }
    }
}
