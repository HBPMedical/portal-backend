package eu.hbp.mip.services;

import eu.hbp.mip.model.DAOs.UserDAO;
import eu.hbp.mip.repositories.UserRepository;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.IDToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.inject.Named;

@Component
@Named("ActiveUserService")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ActiveUserService {

    @Value("#{'${authentication.enabled}'}")
    private boolean authentication;

    private UserDAO user;

    @Autowired
    private UserRepository userRepository;

    /**
     * Fetches the details of the active user.
     * If the user doesn't exist, it's created on the fly from the auth token.
     *
     * @return the userDAO
     */
    public UserDAO getActiveUser() {

        // User already loaded
        if (user != null)
            return user;

        // If Authentication is OFF, create anonymous user with accepted NDA
        if (!authentication) {
            user = new UserDAO("anonymous", "anonymous", "anonymous@anonymous.com");
            user.setAgreeNDA(true);
            userRepository.save(user);
            return user;
        }

        // If authentication is ON get user info from Token
        KeycloakPrincipal keycloakPrincipal =
                (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        IDToken idToken = keycloakPrincipal.getKeycloakSecurityContext().getIdToken();
        UserDAO userInDatabase = userRepository.findByUsername(idToken.getPreferredUsername());
        if (userInDatabase != null) {
            user = userInDatabase;
        } else {
            UserDAO newUser = new UserDAO(idToken.getPreferredUsername(), idToken.getName(), idToken.getEmail());
            userRepository.save(newUser);
            user = newUser;
        }
        return user;
    }

    public UserDAO agreeToNDA() {
        // Fetch the active user
        getActiveUser();

        user.setAgreeNDA(true);
        userRepository.save(user);

        return user;
    }
}
