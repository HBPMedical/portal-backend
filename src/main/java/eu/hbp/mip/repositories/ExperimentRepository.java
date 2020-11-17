package eu.hbp.mip.repositories;

import eu.hbp.mip.models.DAOs.ExperimentDAO;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by mirco on 11.07.16.
 */

public interface ExperimentRepository extends CrudRepository<ExperimentDAO, UUID>, JpaSpecificationExecutor<ExperimentDAO>
{
    Optional<ExperimentDAO> findByUuid(UUID experimentUuid);
}
