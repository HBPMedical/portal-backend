package hbp.mip.controllers;

import hbp.mip.models.DTOs.PathologyDTO;
import hbp.mip.services.ActiveUserService;
import hbp.mip.services.PathologyService;
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
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /pathologies");
        List<PathologyDTO> pathologies = pathologyService.getPathologies(authentication, logger);

        String userPathologiesSTR = pathologies.stream().map(PathologyDTO::code)
                .collect(Collectors.joining(", "));
        logger.LogUserAction("Access given to " + pathologies.size() + " pathologies: [" + userPathologiesSTR + "].");

        return ResponseEntity.ok(pathologies);
    }

}
