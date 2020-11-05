package eu.hbp.mip.controllers;

import eu.hbp.mip.model.DTOs.ExperimentDTO;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.services.ExperimentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
    This api is creating experiments and running it's algorithm on the
    exareme or galaxy clients.
 */

@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/experiments")
public class ExperimentApi {

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private ExperimentService experimentService;

    @ApiOperation(value = "Get experiments", response = ExperimentDTO.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getExperiments(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "algorithm", required = false) String algorithm,
            @RequestParam(name = "shared", required = false) Boolean shared,
            @RequestParam(name = "viewed", required = false) Boolean viewed
    ) {
        return experimentService.getExperiments(name, algorithm, shared, viewed, "(GET) /experiments");
    }

    @ApiOperation(value = "Get an experiment", response = ExperimentDTO.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public ResponseEntity<String> getExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        return experimentService.getExperiment(uuid, "(GET) /experiments/{uuid}");
    }

    @ApiOperation(value = "Create an experiment", response = ExperimentDTO.class)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        return experimentService.createExperiment(authentication, experimentDTO, "(POST) /experiments");
    }

    @ApiOperation(value = "Update an experiment", response = ExperimentDTO.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.PATCH)
    public ResponseEntity<String> updateExperiment(@RequestBody ExperimentDTO experimentDTO,@ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        return experimentService.updateExperiment(uuid, experimentDTO, "(PATCH) /experiments/{uuid}");
    }

    @ApiOperation(value = "Delete an experiment", response = boolean.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        return experimentService.deleteExperiment(uuid, "(DELETE) /experiments/{uuid}");
    }
}