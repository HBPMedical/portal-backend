package hbp.mip.repositories;

import hbp.mip.models.DAOs.UserDAO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

@RestResource(exported = false)
public interface UserRepository extends CrudRepository<UserDAO, String> {
    UserDAO findByUsername(String username);
}
