package eu.hbp.mip.repositories;

import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import eu.hbp.mip.utils.Exceptions.ExperimentNotFoundException;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import eu.hbp.mip.utils.JsonConverters;
import eu.hbp.mip.utils.Logger;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.UUID;


public interface ExperimentRepository extends CrudRepository<ExperimentDAO, UUID>, JpaSpecificationExecutor<ExperimentDAO>
{
    ExperimentDAO findByUuid(UUID experimentUuid);
    /**
     * The loadExperiment access the database and load the information of a specific experiment
     *
     * @param uuid is the id of the experiment to be retrieved
     * @return the experiment information that was retrieved from database
     */
    default ExperimentDAO loadExperiment(String uuid, Logger logger) {
        UUID experimentUuid;
        ExperimentDAO experimentDAO;

        try {
            experimentUuid = UUID.fromString(uuid);
        } catch (Exception e) {
            logger.LogUserAction( e.getMessage());
            throw new BadRequestException(e.getMessage());
        }

        experimentDAO = findByUuid(experimentUuid);
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
    default ExperimentDAO createExperimentInTheDatabase(ExperimentDTO experimentDTO, UserDAO user, Logger logger) {

        ExperimentDAO experimentDAO = new ExperimentDAO();
        experimentDAO.setUuid(UUID.randomUUID());
        experimentDAO.setCreatedBy(user);
        experimentDAO.setAlgorithm(JsonConverters.convertObjectToJsonString(experimentDTO.getAlgorithm()));
        experimentDAO.setAlgorithmId(experimentDTO.getAlgorithm().getName());
        experimentDAO.setName(experimentDTO.getName());
        experimentDAO.setStatus(ExperimentDAO.Status.pending);

        try {
            save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction("Attempted to save changes to database but an error occurred  : " + e.getMessage() + ".");
            throw new InternalServerError(e.getMessage());
        }
        return experimentDAO;
    }

    default void finishExperiment(ExperimentDAO experimentDAO, Logger logger) {
        experimentDAO.setFinished(new Date());

        try {
            save(experimentDAO);
        } catch (Exception e) {
            logger.LogUserAction(
                    "Attempted to save changes to database but an error occurred  : " + e.getMessage() + "."
            );
            throw new InternalServerError(e.getMessage());
        }
    }

}
