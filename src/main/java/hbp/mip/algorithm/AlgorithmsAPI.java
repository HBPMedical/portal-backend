package hbp.mip.algorithm;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/algorithms", produces = {APPLICATION_JSON_VALUE})
public class AlgorithmsAPI {

    private final AlgorithmService algorithmService;
    private final ActiveUserService activeUserService;

    public AlgorithmsAPI(ActiveUserService activeUserService, AlgorithmService algorithmService) {
        this.activeUserService = activeUserService;
        this.algorithmService = algorithmService;
    }

    @GetMapping
    public ResponseEntity<List<AlgorithmSpecificationDTO>> getAlgorithms(Authentication authentication, @RequestParam Map<String, String> allParams) {
        String username = activeUserService.getActiveUser(authentication).username();

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "GET",
                "endpoint", "/algorithms",
                "parameters", allParams
        );

        Logger logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        // Fetching algorithms
        List<AlgorithmSpecificationDTO> algorithms = algorithmService.getAlgorithms(logger);

        // Logging HTTP response
        Map<String, Object> responseLog = Map.of(
                "response", Map.of(
                        "status", 200,
                        "bodySize", algorithms.size() // Just logging count; modify if needed
                )
        );
        logger.info("HTTP response", responseLog);

        return ResponseEntity.ok(algorithms);
    }
}
