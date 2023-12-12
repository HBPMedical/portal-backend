package hbp.mip.algorithm;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<List<AlgorithmSpecificationDTO>> getAlgorithms(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /algorithms");
        logger.info("Request for algorithms.");
        List<AlgorithmSpecificationDTO> algorithms = algorithmService.getAlgorithms(logger);
        logger.info("Algorithms returned: " + algorithms.size());
        return ResponseEntity.ok(algorithms);
    }
}
