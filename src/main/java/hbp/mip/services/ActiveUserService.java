package hbp.mip.services;

import hbp.mip.models.DAOs.UserDAO;
import hbp.mip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ActiveUserService {

    @Value("${authentication.enabled}")
    private boolean authenticationIsEnabled;

    private UserDAO activeUserDetails;

    private final UserRepository userRepository;

    public ActiveUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches the details of the active user.
     * If the user doesn't exist, it's created on the fly from the auth token.
     *
     * @return the userDAO
     */
    public UserDAO getActiveUser(Authentication authentication) {
        // TODO getActiveUser should not be called in so many places.
        // It should be called in the controller and then passed internally in the other methods as UserDTO.
        // TODO getActiveUser should return a UserDTO instead of a DAO

        if (activeUserDetails != null)
            return activeUserDetails;

        // If Authentication is OFF, create anonymous user with accepted NDA
        if (!authenticationIsEnabled) {
            activeUserDetails = new UserDAO("anonymous", "anonymous", "anonymous@anonymous.com", "anonymousId");
            activeUserDetails.setAgreeNDA(true);
            userRepository.save(activeUserDetails);
            return activeUserDetails;
        }


        OidcUserInfo userinfo = ((DefaultOidcUser) authentication.getPrincipal()).getUserInfo();
        activeUserDetails = new UserDAO(userinfo.getPreferredUsername(), userinfo.getFullName(), userinfo.getEmail(), userinfo.getSubject());

        UserDAO activeUserDatabaseDetails = userRepository.findByUsername(activeUserDetails.getUsername());
        if (activeUserDatabaseDetails != null) {
            if ((!Objects.equals(activeUserDetails.getEmail(), activeUserDatabaseDetails.getEmail()))
                    || !Objects.equals(activeUserDetails.getFullname(), activeUserDatabaseDetails.getFullname())
            ) {
                // Fullname and email are the only values allowed to change.
                // username is the PK in our database and subjectid is the PK in keycloak
                activeUserDatabaseDetails.setFullname(activeUserDetails.getFullname());
                activeUserDatabaseDetails.setEmail(activeUserDetails.getEmail());
            }
            activeUserDetails = activeUserDatabaseDetails;
        }
        userRepository.save(activeUserDetails);
        return activeUserDetails;
    }

    public UserDAO agreeToNDA(Authentication authentication) {
        getActiveUser(authentication);

        activeUserDetails.setAgreeNDA(true);
        userRepository.save(activeUserDetails);

        return activeUserDetails;
    }
}
