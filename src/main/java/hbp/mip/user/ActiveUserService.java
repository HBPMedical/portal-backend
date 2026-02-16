package hbp.mip.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class ActiveUserService {

    private final UserRepository userRepository;
    @Value("${authentication.enabled}")

    private boolean authenticationIsEnabled;

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
        UserDAO activeUserDAO;
        if (authenticationIsEnabled) {
            // If Authentication is ON, get user details from authentication info.
            activeUserDAO = buildUserFromAuthentication(authentication);

            UserDAO activeUserDatabaseDetails = userRepository.findByUsername(activeUserDAO.getUsername());
            if (activeUserDatabaseDetails != null) {
                boolean changed = false;
                if ((!Objects.equals(activeUserDAO.getEmail(), activeUserDatabaseDetails.getEmail()))
                        || !Objects.equals(activeUserDAO.getFullname(), activeUserDatabaseDetails.getFullname())
                ) {
                    // Fullname and email are the only values allowed to change.
                    // username is the PK in our database and subjectid is the PK in keycloak
                    activeUserDatabaseDetails.setFullname(activeUserDAO.getFullname());
                    activeUserDatabaseDetails.setEmail(activeUserDAO.getEmail());
                    changed = true;
                }
                if (changed) {
                    userRepository.save(activeUserDatabaseDetails);
                }
                activeUserDAO = activeUserDatabaseDetails;
            } else {
                userRepository.save(activeUserDAO);
            }

        } else {
            // If Authentication is OFF, ensure anonymous user exists with accepted NDA.
            UserDAO anonymous = userRepository.findByUsername("anonymous");
            if (anonymous == null) {
                anonymous = new UserDAO("anonymous", "anonymous", "anonymous@anonymous.com", "anonymousId");
                anonymous.setAgreeNDA(true);
                userRepository.save(anonymous);
            } else if (!Boolean.TRUE.equals(anonymous.getAgreeNDA())) {
                anonymous.setAgreeNDA(true);
                userRepository.save(anonymous);
            }
            activeUserDAO = anonymous;
        }

        return new UserDTO(activeUserDAO);
    }

    public UserDTO agreeToNDA(Authentication authentication) {
        UserDTO userDTO = getActiveUser(authentication);

        UserDAO userDAO = new UserDAO(userDTO);
        userDAO.setAgreeNDA(true);
        userRepository.save(userDAO);

        return new UserDTO(userDAO);
    }

    private static UserDAO buildUserFromAuthentication(Authentication authentication) {
        Object principal = authentication != null ? authentication.getPrincipal() : null;

        // Browser login flow (session-based): OIDC user.
        if (principal instanceof DefaultOidcUser oidcUser) {
            OidcUserInfo userinfo = oidcUser.getUserInfo();
            return new UserDAO(
                    userinfo.getPreferredUsername(),
                    userinfo.getFullName(),
                    userinfo.getEmail(),
                    userinfo.getSubject()
            );
        }

        // API client flow (token-based): JWT bearer.
        Jwt jwt = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            jwt = jwtAuth.getToken();
        } else if (principal instanceof Jwt pJwt) {
            jwt = pJwt;
        }

        if (jwt != null) {
            String username = firstNonBlank(
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("username"),
                    jwt.getSubject()
            );
            String fullName = firstNonBlank(
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("given_name")
            );
            String email = firstNonBlank(jwt.getClaimAsString("email"), username + "@unknown.local");
            String subject = firstNonBlank(jwt.getSubject(), username);
            return new UserDAO(username, fullName, email, subject);
        }

        // Fallback: keep behavior explicit rather than ClassCastException.
        String fallbackName = authentication != null ? authentication.getName() : "unknown";
        return new UserDAO(fallbackName, fallbackName, fallbackName + "@unknown.local", fallbackName);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
