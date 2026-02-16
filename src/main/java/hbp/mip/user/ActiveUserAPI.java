package hbp.mip.user;

import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/activeUser", produces = { APPLICATION_JSON_VALUE })
public class ActiveUserAPI {

    private final ActiveUserService activeUserService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public ActiveUserAPI(ActiveUserService activeUserService, OAuth2AuthorizedClientService authorizedClientService) {
        this.activeUserService = activeUserService;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping
    public ResponseEntity<UserDTO> getTheActiveUser(Authentication authentication) {
        UserDTO activeUser = activeUserService.getActiveUser(authentication);
        Logger logger = new Logger(activeUser.username(), "(GET) /activeUser");
        logger.info("User details returned.");
        return ResponseEntity.ok(activeUser);
    }

    @PostMapping(value = "/agreeNDA")
    public ResponseEntity<UserDTO> activeUserServiceAgreesToNDA(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).username(),
                "(POST) /activeUser/agreeNDA");
        logger.info("User agreed to the NDA.");
        return ResponseEntity.ok(activeUserService.agreeToNDA(authentication));
    }

    @GetMapping(value = "/token")
    public ResponseEntity<String> getActiveUserToken(Authentication authentication) {
        if (authentication == null) {
            Logger logger = new Logger("anonymous", "(GET) /activeUser/token");
            logger.warn("Authentication is missing.");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // Bearer-token flow: JWT presented directly to this API.
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return ResponseEntity.ok(jwtAuth.getToken().getTokenValue());
        }

        // Browser session flow: return OAuth2 access token from authorized client.
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauth2Auth.getAuthorizedClientRegistrationId(),
                    oauth2Auth.getName()
            );
            if (client != null && client.getAccessToken() != null
                    && StringUtils.hasText(client.getAccessToken().getTokenValue())) {
                return ResponseEntity.ok(client.getAccessToken().getTokenValue());
            }
        }

        Logger logger = new Logger("unknown", "(GET) /activeUser/token");
        logger.warn("Could not extract access token from authentication object: " + authentication.getClass().getName());
        return ResponseEntity.status(404).body("Token not found");
    }
}
