package hbp.mip.pathology;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
public class PathologiesAPI {

    private final PathologyService pathologyService;

    private final ActiveUserService activeUserService;

    public PathologiesAPI(ActiveUserService activeUserService, PathologyService pathologyService) {
        this.activeUserService = activeUserService;
        this.pathologyService = pathologyService;
    }

    @GetMapping
    public ResponseEntity<List<PathologyDTO>> getPathologies(Authentication authentication) {
        var username = activeUserService.getActiveUser(authentication).username();

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "GET",
                "endpoint", "/pathologies"
        );

        var logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        // Fetch pathologies
        var pathologies = pathologyService.getPathologies(authentication, logger);

        // Generate formatted pathology codes for logging
        String userPathologiesSTR = pathologies.stream()
                .map(PathologyDTO::code)
                .collect(Collectors.joining(", "));

        // Logging response with correct data type
        Map<String, Object> responseLog = Map.of(
                "response", Map.of(
                        "status", 200,
                        "pathologiesCount", pathologies.size(),
                        "pathologies", userPathologiesSTR
                )
        );
        logger.info("HTTP response", responseLog);

        return ResponseEntity.ok(pathologies);
    }
}
