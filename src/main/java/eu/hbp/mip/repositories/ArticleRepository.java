package eu.hbp.mip.repositories;

import eu.hbp.mip.model.DAOs.ArticleDAO;
import eu.hbp.mip.model.DAOs.UserDAO;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by mirco on 11.07.16.
 */

public interface ArticleRepository extends CrudRepository<ArticleDAO, String> {
    Long countByTitle(String title);

    Iterable<ArticleDAO> findByCreatedBy(UserDAO user);

    Iterable<ArticleDAO> findByStatusOrCreatedBy(String status, UserDAO user);
}
