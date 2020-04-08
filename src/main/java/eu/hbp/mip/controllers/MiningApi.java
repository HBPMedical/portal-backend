package eu.hbp.mip.controllers;
import eu.hbp.mip.utils.HTTPUtil;

import com.google.gson.Gson;

import eu.hbp.mip.model.Mining;
import eu.hbp.mip.model.User;
import eu.hbp.mip.model.UserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.hbp.mip.utils.UserActionLogging;

import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by mirco on 06.01.17.
 */
@RestController
@RequestMapping(value = "/mining", produces = { APPLICATION_JSON_VALUE })
@Api(value = "/mining", description = "the mining API")
public class MiningApi {

    private static final Gson gson = new Gson();

    @Autowired
    private UserInfo userInfo;

    @Value("#{'${services.exareme.queryExaremeUrl:http://localhost:9090/mining/query}'}")
    public String queryExaremeUrl;

    @ApiOperation(value = "Create a histogram on Exareme", response = String.class)
    @RequestMapping(value = "/histograms", method = RequestMethod.POST)
    public ResponseEntity runExaremeHistograms(@RequestBody List<HashMap<String, String>> queryList) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run an histogram", "");

        String query = gson.toJson(queryList);
        String url = queryExaremeUrl + "/" + "MULTIPLE_HISTOGRAMS";

        try {
            StringBuilder results = new StringBuilder();
            int code = HTTPUtil.sendPost(url, query, results);

            return ResponseEntity.ok(gson.toJson(results.toString()));
        } catch (IOException e) {
            return new ResponseEntity<>("Not found", HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Create a descriptive statistic on Exareme", response = String.class)
    @RequestMapping(value = "/descriptive_stats", method = RequestMethod.POST)
    public ResponseEntity runExaremeDescriptiveStats(@RequestBody List<HashMap<String, String>> queryList) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Run descriptive stats", "");

        String query = gson.toJson(queryList);
        String url = queryExaremeUrl + "/" + "DESCRIPTIVE_STATS";

        try {
            StringBuilder results = new StringBuilder();
            int code = HTTPUtil.sendPost(url, query, results);

            return ResponseEntity.ok(gson.toJson(results.toString()));
        } catch (IOException e) {
            return new ResponseEntity<>("Not found", HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Check if a formula is valid", response = String.class)
    @RequestMapping(value = "/checkFormula", method = RequestMethod.POST)
    public ResponseEntity checkFormulaValidity(String formula) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Check Formula Validity", "");

        return ResponseEntity.ok("");
    }
}
