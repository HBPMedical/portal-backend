package eu.hbp.mip.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.hbp.mip.configuration.SecurityConfiguration;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

@RestController
public class SecurityApi {

    private static final Gson gson = new Gson();

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private SecurityConfiguration securityConfiguration;

    // TODO Fix no authentication instance
    @RequestMapping(path = "/login/hbp", method = RequestMethod.GET)
    @ConditionalOnExpression("${authentication.enabled:0}")
    public void noLogin(HttpServletResponse httpServletResponse) throws IOException {
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /user/login/hbp", "Unauthorized login.");
        httpServletResponse.sendRedirect(securityConfiguration.getFrontendRedirectAfterLogin());
    }

    @Value("#{'${services.galaxy.galaxyUsername:admin}'}")
    private String galaxyUsername;

    @Value("#{'${services.galaxy.galaxyPassword:password}'}")
    private String galaxyPassword;

    @Value("#{'${services.galaxy.galaxpathoyContext:nativeGalaxy}'}")
    private String galaxyContext;

    /**
     * Get Galaxy Reverse Proxy basic access token.
     *
     * @return Return a @{@link ResponseEntity} with the token.
     */
    @RequestMapping(path = "/galaxy", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasRole('Data Manager')")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getGalaxyConfiguration() {
        String stringEncoded = Base64.getEncoder().encodeToString((galaxyUsername + ":" + galaxyPassword).getBytes());
        JsonObject object = new JsonObject();
        object.addProperty("authorization", stringEncoded);
        object.addProperty("context", galaxyContext);
        Logging.LogUserAction(activeUserService.getActiveUser().getUsername(), "(GET) /user/galaxy", "Successfully Loaded galaxy information.");

        return ResponseEntity.ok(gson.toJson(object));
    }
}
