package eu.hbp.mip.controllers;

import eu.hbp.mip.models.DTOs.ExaremeAlgorithmDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.services.AlgorithmService;
import eu.hbp.mip.utils.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/algorithms", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/algorithms")
public class AlgorithmsAPI {

    private final AlgorithmService algorithmService;
    private final ActiveUserService activeUserService;
    public AlgorithmsAPI(ActiveUserService activeUserService, AlgorithmService algorithmService) {
        this.activeUserService = activeUserService;
        this.algorithmService = algorithmService;
    }

    @ApiOperation(value = "List all algorithms", response = String.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ExaremeAlgorithmDTO>> getAlgorithms() {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /algorithms");
        logger.LogUserAction("Executing...");
        List<ExaremeAlgorithmDTO> allowedAlgorithms = algorithmService.getAlgorithms(logger);

        logger.LogUserAction("Successfully listed " + allowedAlgorithms.size() + " algorithms");
        return ResponseEntity.ok(allowedAlgorithms);
    }
}
