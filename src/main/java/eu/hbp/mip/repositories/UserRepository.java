package eu.hbp.mip.repositories;

import eu.hbp.mip.models.DAOs.UserDAO;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserDAO, String> {
    UserDAO findByUsername(String username);
}
