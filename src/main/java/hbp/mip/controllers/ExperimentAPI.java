package hbp.mip.controllers;


import hbp.mip.models.DTOs.ExperimentDTO;
import hbp.mip.models.DTOs.UserDTO;
import hbp.mip.services.ActiveUserService;
import hbp.mip.services.ExperimentService;
import hbp.mip.utils.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
public class ExperimentAPI {

    private final ExperimentService experimentService;
    private final ActiveUserService activeUserService;

    public ExperimentAPI(
            ExperimentService experimentService,
            ActiveUserService activeUserService
    ) {
        this.experimentService = experimentService;
        this.activeUserService = activeUserService;
    }

    @GetMapping
    public ResponseEntity<Object> getExperiments(Authentication authentication,
                                                 @RequestParam(name = "name", required = false) String name,
                                                 @RequestParam(name = "algorithm", required = false) String algorithm,
                                                 @RequestParam(name = "shared", required = false) Boolean shared,
                                                 @RequestParam(name = "viewed", required = false) Boolean viewed,
                                                 @RequestParam(name = "includeShared", required = false, defaultValue = "true") boolean includeShared,
                                                 @RequestParam(name = "orderBy", required = false, defaultValue = "created") String orderBy,
                                                 @RequestParam(name = "descending", required = false, defaultValue = "true") Boolean descending,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> experiments = experimentService.getExperiments(authentication,
                name,
                algorithm,
                shared,
                viewed,
                includeShared,
                page,
                size,
                orderBy,
                descending,
                new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /experiments"));
        return new ResponseEntity<>(experiments, HttpStatus.OK);
    }


    @GetMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> getExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        ExperimentDTO experimentDTO = experimentService.getExperiment(
                authentication, uuid,
                new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /experiments/{uuid}")
        );
        return new ResponseEntity<>(experimentDTO, HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<ExperimentDTO> createExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        experimentDTO = experimentService.createExperiment(
                authentication, experimentDTO,
                new Logger(activeUserService.getActiveUser(authentication).username(), "(POST) /experiments")
        );
        return new ResponseEntity<>(experimentDTO, HttpStatus.CREATED);
    }


    @PatchMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> updateExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO, @PathVariable("uuid") String uuid) {
        UserDTO user = activeUserService.getActiveUser(authentication);
        experimentDTO = experimentService.updateExperiment(
                user,
                uuid,
                experimentDTO,
                new Logger(user.username(), "(PATCH) /experiments/{uuid}")
        );
        return new ResponseEntity<>(experimentDTO, HttpStatus.OK);
    }


    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        UserDTO user = activeUserService.getActiveUser(authentication);
        experimentService.deleteExperiment(
                user,
                uuid,
                new Logger(user.username(), "(DELETE) /experiments/{uuid}")
        );
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping(value = "/transient")
    public ResponseEntity<ExperimentDTO> createTransientExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        experimentDTO = experimentService.runTransientExperiment(
                authentication,
                experimentDTO,
                new Logger(activeUserService.getActiveUser(authentication).username(), "(POST) /experiments/transient")
        );
        return new ResponseEntity<>(experimentDTO, HttpStatus.OK);
    }
}