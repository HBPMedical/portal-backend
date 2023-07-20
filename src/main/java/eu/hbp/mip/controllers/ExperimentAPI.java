package eu.hbp.mip.controllers;


import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.services.ExperimentService;
import eu.hbp.mip.utils.JsonConverters;
import eu.hbp.mip.utils.Logger;
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

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getExperiments(Authentication authentication,
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
        Map experiments = experimentService.getExperiments(authentication,
                name,
                algorithm,
                shared,
                viewed,
                includeShared,
                page,
                size,
                orderBy,
                descending,
                new Logger(activeUserService.getActiveUser().getUsername(),"(GET) /experiments"));
        return new ResponseEntity(experiments, HttpStatus.OK);
    }


    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public ResponseEntity<String> getExperiment(Authentication authentication, @PathVariable("uuid") String uuid) {
        ExperimentDTO experimentDTO = experimentService.getExperiment(authentication, uuid, new Logger(activeUserService.getActiveUser().getUsername(),"(GET) /experiments/{uuid}"));
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }


    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        experimentDTO = experimentService.createExperiment(authentication, experimentDTO, new Logger(activeUserService.getActiveUser().getUsername(),"(POST) /experiments"));
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{uuid}", method = RequestMethod.PATCH)
    public ResponseEntity<String> updateExperiment(@RequestBody ExperimentDTO experimentDTO, @PathVariable("uuid") String uuid) {
        experimentDTO = experimentService.updateExperiment(uuid, experimentDTO, new Logger(activeUserService.getActiveUser().getUsername(),"(PATCH) /experiments/{uuid}"));
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }


    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteExperiment(@PathVariable("uuid") String uuid) {
        experimentService.deleteExperiment(uuid, new Logger(activeUserService.getActiveUser().getUsername(), "(DELETE) /experiments/{uuid}"));
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/transient", method = RequestMethod.POST)
    public ResponseEntity<String> createTransientExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        experimentDTO = experimentService.
                runTransientExperiment(authentication, experimentDTO, new Logger(activeUserService.getActiveUser().getUsername(), "(POST) /experiments/transient"));
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }
}