package hbp.mip.experiment;


import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.JsonConverters;
import hbp.mip.utils.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ExperimentsDTO> getExperiments(Authentication authentication,
                                                         @RequestParam(name = "name", required = false) String name,
                                                         @RequestParam(name = "algorithm", required = false) String algorithm,
                                                         @RequestParam(name = "shared", required = false) Boolean shared,
                                                         @RequestParam(name = "viewed", required = false) Boolean viewed,
                                                         @RequestParam(name = "includeShared", required = false, defaultValue = "true") boolean includeShared,
                                                         @RequestParam(name = "mine", required = false, defaultValue = "false") boolean mine,
                                                         @RequestParam(name = "orderBy", required = false, defaultValue = "created") String orderBy,
                                                         @RequestParam(name = "descending", required = false, defaultValue = "true") Boolean descending,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size
    ) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /experiments");
        logger.info(
                "Request for experiments with parameters: " +
                        "name -> " + name +
                        " , algorithm -> " + algorithm +
                        " , shared -> " + shared +
                        " , viewed -> " + viewed +
                        " , includeShared -> " + includeShared +
                        " , mine -> " + mine +
                        " , orderBy -> " + orderBy +
                        " , descending -> " + descending +
                        " , page -> " + page +
                        " , size -> " + size
        );
        var experimentsDTO = experimentService.getExperiments(authentication,
                name,
                algorithm,
                shared,
                viewed,
                includeShared,
                mine,
                page,
                size,
                orderBy,
                descending,
                logger
        );
        logger.info("Experiments returned: " + experimentsDTO.experiments().size());
        return new ResponseEntity<>(experimentsDTO, HttpStatus.OK);
    }


    @GetMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> getExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /experiments/" + uuid);
        logger.info("Request for experiment with id: " + uuid);
        var experimentResponse = experimentService.getExperiment(authentication, uuid, logger);
        logger.info("Experiment returned.");
        return new ResponseEntity<>(experimentResponse, HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<ExperimentDTO> createExperiment(Authentication authentication, @RequestBody ExperimentExecutionDTO experimentExecutionDTO) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(POST) /experiments");
        logger.info("Request for experiment creation. RequestBody: " + JsonConverters.convertObjectToJsonString(experimentExecutionDTO));
        var experimentResponse = experimentService.createExperiment(authentication, experimentExecutionDTO, logger);
        logger.info("Experiment created with id: " + experimentResponse.uuid());
        return new ResponseEntity<>(experimentResponse, HttpStatus.CREATED);
    }


    @PatchMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> updateExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentRequest, @PathVariable("uuid") String uuid) {
        var user = activeUserService.getActiveUser(authentication);
        var logger = new Logger(user.username(), "(PATCH) /experiments/" + uuid);
        logger.info("Request for experiment update with id: " + uuid + ".  Request Body: " + JsonConverters.convertObjectToJsonString(experimentRequest));
        var experimentResponse = experimentService.updateExperiment(user, uuid, experimentRequest, logger);
        logger.info("Experiment updated. Id: " + uuid);
        return new ResponseEntity<>(experimentResponse, HttpStatus.OK);
    }


    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        var user = activeUserService.getActiveUser(authentication);
        var logger = new Logger(user.username(), "(DELETE) /experiments/" + uuid);
        logger.info("Request for experiment deletion with id: " + uuid);
        experimentService.deleteExperiment(user, uuid, logger);
        logger.info("Experiment deleted. Id: " + uuid);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping(value = "/transient")
    public ResponseEntity<ExperimentDTO> createTransientExperiment(Authentication authentication, @RequestBody ExperimentExecutionDTO experimentExecutionDTO) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(POST) /experiments/transient");
        logger.info("Request for transient experiment creation. RequestBody: " + JsonConverters.convertObjectToJsonString(experimentExecutionDTO));

        var experimentResponse = experimentService.runTransientExperiment(
                authentication,
                experimentExecutionDTO,
                logger
        );

        logger.info(
                "Experiment (transient) finished. " +
                        " Status: " + experimentResponse.status() +
                        " Result: " + experimentResponse.result()
        );

        return new ResponseEntity<>(experimentResponse, HttpStatus.OK);
    }
}