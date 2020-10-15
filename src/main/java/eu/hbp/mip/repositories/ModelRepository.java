package eu.hbp.mip.repositories;

import eu.hbp.mip.model.Model;
import eu.hbp.mip.model.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by mirco on 11.07.16.
 */

public interface ModelRepository extends CrudRepository<Model, String> {
    Long countByTitle(String Title);

    Iterable<Model> findByCreatedByOrderByCreatedAt(User user);

    Iterable<Model> findByValidOrCreatedByOrderByCreatedAt(Boolean valid, User user);
}
