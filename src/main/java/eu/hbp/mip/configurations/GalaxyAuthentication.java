package eu.hbp.mip.configurations;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
public class GalaxyAuthentication {

    private final ActiveUserService activeUserService;

    @Value("#{'${services.galaxy.galaxyUsername:admin}'}")
    private String galaxyUsername;

    @Value("#{'${services.galaxy.galaxyPassword:password}'}")
    private String galaxyPassword;

    @Value("#{'${services.galaxy.galaxpathoyContext:nativeGalaxy}'}")
    private String galaxyContext;

    public GalaxyAuthentication(ActiveUserService activeUserService) {
        this.activeUserService = activeUserService;
    }

    /**
     * Get Galaxy Reverse Proxy basic access token.
     *
     * @return Return a @{@link ResponseEntity} with the token.
     */
    @RequestMapping(path = "/galaxy", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasRole('WORKFLOW_ADMIN')")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity getGalaxyConfiguration() {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /user/galaxy");
        String stringEncoded = Base64.getEncoder().encodeToString((galaxyUsername + ":" + galaxyPassword).getBytes());
        JsonObject object = new JsonObject();
        object.addProperty("authorization", stringEncoded);
        object.addProperty("context", galaxyContext);
        logger.LogUserAction("Successfully Loaded galaxy information.");

        return ResponseEntity.ok(new Gson().toJson(object));
    }
}
