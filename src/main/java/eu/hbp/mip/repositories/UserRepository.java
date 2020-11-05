package eu.hbp.mip.repositories;

import eu.hbp.mip.model.DAOs.UserDAO;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by mirco on 11.07.16.
 */

public interface UserRepository extends CrudRepository<UserDAO, String> {

    UserDAO findByUsername(String username);
}
