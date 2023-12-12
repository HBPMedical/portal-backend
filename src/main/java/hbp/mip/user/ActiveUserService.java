package hbp.mip.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ActiveUserService {

    private final UserRepository userRepository;
    @Value("${authentication.enabled}")

    private boolean authenticationIsEnabled;

    private UserDTO activeUserDetails;

    public ActiveUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches the details of the active user.
     * If the user doesn't exist, it's created on the fly from the auth token.
     *
     * @return the userDAO
     */
    public UserDTO getActiveUser(Authentication authentication) {
        if (activeUserDetails != null)
            return activeUserDetails;

        UserDAO activeUserDAO;
        if (authenticationIsEnabled) {
            // If Authentication is ON, get user details from authentication info.
            OidcUserInfo userinfo = ((DefaultOidcUser) authentication.getPrincipal()).getUserInfo();
            activeUserDAO = new UserDAO(userinfo.getPreferredUsername(), userinfo.getFullName(), userinfo.getEmail(), userinfo.getSubject());

            UserDAO activeUserDatabaseDetails = userRepository.findByUsername(activeUserDAO.getUsername());
            if (activeUserDatabaseDetails != null) {
                if ((!Objects.equals(activeUserDAO.getEmail(), activeUserDatabaseDetails.getEmail()))
                        || !Objects.equals(activeUserDAO.getFullname(), activeUserDatabaseDetails.getFullname())
                ) {
                    // Fullname and email are the only values allowed to change.
                    // username is the PK in our database and subjectid is the PK in keycloak
                    activeUserDatabaseDetails.setFullname(activeUserDAO.getFullname());
                    activeUserDatabaseDetails.setEmail(activeUserDAO.getEmail());
                }
                activeUserDAO = activeUserDatabaseDetails;
            }
            userRepository.save(activeUserDAO);

        } else {
            // If Authentication is OFF, create anonymous user with accepted NDA
            activeUserDAO = new UserDAO("anonymous", "anonymous", "anonymous@anonymous.com", "anonymousId");
            activeUserDAO.setAgreeNDA(true);
            userRepository.save(activeUserDAO);
        }

        activeUserDetails = new UserDTO(activeUserDAO);
        return activeUserDetails;
    }

    public UserDTO agreeToNDA(Authentication authentication) {
        UserDTO userDTO = getActiveUser(authentication);

        UserDAO userDAO = new UserDAO(userDTO);
        userDAO.setAgreeNDA(true);
        userRepository.save(userDAO);

        activeUserDetails = new UserDTO(userDAO);
        return activeUserDetails;
    }
}
