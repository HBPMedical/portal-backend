package eu.hbp.mip.repositories;

import eu.hbp.mip.model.DAOs.ExperimentDAO;
import eu.hbp.mip.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Created by mirco on 11.07.16.
 */

public interface ExperimentRepository extends CrudRepository<ExperimentDAO, UUID> {
    Iterable<ExperimentDAO> findByCreatedBy(User user);

    Iterable<ExperimentDAO> findByShared(Boolean shared);
}
