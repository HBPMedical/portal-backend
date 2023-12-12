package hbp.mip.pathology;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pathologies")
public class PathologiesAPI {

    private final PathologyService pathologyService;

    private final ActiveUserService activeUserService;

    public PathologiesAPI(ActiveUserService activeUserService, PathologyService pathologyService) {
        this.activeUserService = activeUserService;
        this.pathologyService = pathologyService;
    }

    @GetMapping
    public ResponseEntity<List<PathologyDTO>> getPathologies(Authentication authentication) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /pathologies");
        logger.info("Request for pathologies.");
        var pathologies = pathologyService.getPathologies(authentication, logger);

        String userPathologiesSTR = pathologies.stream().map(PathologyDTO::code)
                .collect(Collectors.joining(", "));
        logger.info("Pathologies returned: " + pathologies.size() + ". [" + userPathologiesSTR + "].");

        return ResponseEntity.ok(pathologies);
    }

}
