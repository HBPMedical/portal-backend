package eu.hbp.mip.controllers;

import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.services.ExperimentService;
import eu.hbp.mip.utils.JsonConverters;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/*
    This api is creating experiments and running it's algorithm on the
    exareme or galaxy clients.
 */

@RestController
@RequestMapping(value = "/experiments", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/experiments")
public class ExperimentAPI {

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private ExperimentService experimentService;

    @ApiOperation(value = "Get experiments", response = ExperimentDTO.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getExperiments(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "algorithm", required = false) String algorithm,
            @RequestParam(name = "shared", required = false) Boolean shared,
            @RequestParam(name = "viewed", required = false) Boolean viewed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        Map experiments = experimentService.getExperiments(name, algorithm, shared, viewed, page, size, "(GET) /experiments");
        return new ResponseEntity(experiments, HttpStatus.OK);
    }


    @ApiOperation(value = "Get an experiment", response = ExperimentDTO.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public ResponseEntity<String> getExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
       ExperimentDTO experimentDTO = experimentService.getExperiment(uuid, "(GET) /experiments/{uuid}");
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }


    @ApiOperation(value = "Create an experiment", response = ExperimentDTO.class)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
        experimentDTO =  experimentService.createExperiment(authentication, experimentDTO, "(POST) /experiments");
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.CREATED);
    }


    @ApiOperation(value = "Create a transient experiment", response = ExperimentDTO.class)
    @RequestMapping(value = "/{transient}",method = RequestMethod.POST)
    public ResponseEntity<String> createTransientExperiment(Authentication authentication, @RequestBody ExperimentDTO experimentDTO) {
            experimentDTO = experimentService.createTransientExperiment(authentication, experimentDTO, "(POST) /experiments/transient");

        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }


    @ApiOperation(value = "Update an experiment", response = ExperimentDTO.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.PATCH)
    public ResponseEntity<String> updateExperiment(@RequestBody ExperimentDTO experimentDTO,@ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        experimentDTO = experimentService.updateExperiment(uuid, experimentDTO, "(PATCH) /experiments/{uuid}");
        return new ResponseEntity<>(JsonConverters.convertObjectToJsonString(experimentDTO), HttpStatus.OK);
    }


    @ApiOperation(value = "Delete an experiment", response = boolean.class)
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteExperiment(
            @ApiParam(value = "uuid", required = true) @PathVariable("uuid") String uuid) {
        experimentService.deleteExperiment(uuid, "(DELETE) /experiments/{uuid}");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}