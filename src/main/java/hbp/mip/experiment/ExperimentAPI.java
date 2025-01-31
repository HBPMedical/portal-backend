package hbp.mip.experiment;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.JsonConverters;
import hbp.mip.utils.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
public class ExperimentAPI {

    private final ExperimentService experimentService;
    private final ActiveUserService activeUserService;

    public ExperimentAPI(ExperimentService experimentService, ActiveUserService activeUserService) {
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
                                                         @RequestParam(name = "orderBy", required = false, defaultValue = "created") String orderBy,
                                                         @RequestParam(name = "descending", required = false, defaultValue = "true") Boolean descending,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        var username = activeUserService.getActiveUser(authentication).username();

        // Create structured request details
        Map<String, Object> parameters = new HashMap<>();
        if (name != null) parameters.put("name", name);
        if (algorithm != null) parameters.put("algorithm", algorithm);
        if (shared != null) parameters.put("shared", shared);
        if (viewed != null) parameters.put("viewed", viewed);
        parameters.put("includeShared", includeShared);
        parameters.put("orderBy", orderBy);
        parameters.put("descending", descending);
        parameters.put("page", page);
        parameters.put("size", size);

        Map<String, Object> requestDetails = new HashMap<>();
        requestDetails.put("method", "GET");
        requestDetails.put("endpoint", "/experiments");
        requestDetails.put("parameters", parameters);

        var logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        var experimentsDTO = experimentService.getExperiments(authentication,
                name,
                algorithm,
                shared,
                viewed,
                includeShared,
                page,
                size,
                orderBy,
                descending,
                logger
        );


        var responseLog = Map.<String, Object>of(
                "response", Map.of(
                        "status", 200,
                        "experimentsCount", experimentsDTO.experiments().size()
                )
        );
        logger.info("HTTP response", responseLog);

        return ResponseEntity.ok(experimentsDTO);
    }

    @GetMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> getExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        var username = activeUserService.getActiveUser(authentication).username();

        Map<String, Object> requestDetails = Map.of(
                "method", "GET",
                "endpoint", "/experiments/" + uuid
        );

        var logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        var experimentResponse = experimentService.getExperiment(authentication, uuid, logger);

        logger.info("Experiment returned.", Map.of("response", Map.of("status", 200, "experimentId", uuid)));
        return ResponseEntity.ok(experimentResponse);
    }

    @PostMapping
    public ResponseEntity<ExperimentDTO> createExperiment(Authentication authentication, @RequestBody ExperimentExecutionDTO experimentExecutionDTO) {
        var username = activeUserService.getActiveUser(authentication).username();

        Map<String, Object> requestDetails = Map.of(
                "method", "POST",
                "endpoint", "/experiments",
                "body", JsonConverters.convertObjectToJsonString(experimentExecutionDTO)
        );

        var logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        var experimentResponse = experimentService.createExperiment(authentication, experimentExecutionDTO, logger);

        logger.info("Experiment created.", Map.of("response", Map.of("status", 201, "experimentId", experimentResponse.uuid())));
        return ResponseEntity.status(HttpStatus.CREATED).body(experimentResponse);
    }

    @PatchMapping(value = "/{uuid}")
    public ResponseEntity<ExperimentDTO> updateExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentRequest, @PathVariable("uuid") String uuid) {
        var user = activeUserService.getActiveUser(authentication);

        Map<String, Object> requestDetails = Map.of(
                "method", "PATCH",
                "endpoint", "/experiments/" + uuid,
                "body", JsonConverters.convertObjectToJsonString(experimentRequest)
        );

        var logger = new Logger(user.username(), requestDetails);
        logger.info("HTTP request");

        var experimentResponse = experimentService.updateExperiment(user, uuid, experimentRequest, logger);

        logger.info("Experiment updated.", Map.of("response", Map.of("status", 200, "experimentId", uuid)));
        return ResponseEntity.ok(experimentResponse);
    }

    @DeleteMapping(value = "/{uuid}")
    public ResponseEntity<Void> deleteExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        var username = activeUserService.getActiveUser(authentication);

        Map<String, Object> requestDetails = Map.of(
                "method", "DELETE",
                "endpoint", "/experiments/" + uuid
        );

        var logger = new Logger(username.username(), requestDetails);
        logger.info("HTTP request");

        experimentService.deleteExperiment(username, uuid, logger);

        logger.info("Experiment deleted.", Map.of("response", Map.of("status", 200, "experimentId", uuid)));
        return ResponseEntity.ok().build();
    }
    @PostMapping(value = "/transient")
    public ResponseEntity<ExperimentDTO> createTransientExperiment(
            Authentication authentication,
            @RequestBody ExperimentExecutionDTO experimentExecutionDTO) {

        var username = activeUserService.getActiveUser(authentication).username();

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "POST",
                "endpoint", "/experiments/transient",
                "body", JsonConverters.convertObjectToJsonString(experimentExecutionDTO)
        );

        var logger = new Logger(username, requestDetails);
        logger.info("HTTP request");

        var experimentResponse = experimentService.runTransientExperiment(authentication, experimentExecutionDTO, logger);

        // Logging response
        Map<String, Object> responseLog = Map.of(
                "response", Map.of(
                        "status", 200,
                        "experimentId", experimentResponse.uuid(),
                        "statusMessage", experimentResponse.status(),
                        "result", experimentResponse.result()
                )
        );
        logger.info("Experiment (transient) finished.", responseLog);

        return ResponseEntity.ok(experimentResponse);
    }
}
