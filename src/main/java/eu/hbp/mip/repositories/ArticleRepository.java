package eu.hbp.mip.repositories;

import eu.hbp.mip.model.Article;
import eu.hbp.mip.model.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by mirco on 11.07.16.
 */

public interface ArticleRepository extends CrudRepository<Article, String> {
    Long countByTitle(String title);

    Iterable<Article> findByCreatedBy(User user);

    Iterable<Article> findByStatusOrCreatedBy(String status, User user);
}
