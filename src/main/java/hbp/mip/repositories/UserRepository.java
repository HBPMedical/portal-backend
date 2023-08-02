package hbp.mip.repositories;

import hbp.mip.models.DAOs.UserDAO;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserDAO, String> {
    UserDAO findByUsername(String username);
}
