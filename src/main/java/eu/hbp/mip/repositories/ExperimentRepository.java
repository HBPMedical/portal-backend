package eu.hbp.mip.repositories;

import eu.hbp.mip.model.Experiment;
import eu.hbp.mip.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Created by mirco on 11.07.16.
 */

public interface ExperimentRepository extends CrudRepository<Experiment, UUID> {
    Iterable<Experiment> findByCreatedBy(User user);

    Iterable<Experiment> findByShared(Boolean shared);
}
