package eu.hbp.mip.controllers;

import eu.hbp.mip.utils.HTTPUtil;

import com.google.gson.Gson;

import eu.hbp.mip.model.UserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.hbp.mip.utils.Logging;


import java.util.*;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by mirco on 06.01.17.
 */
@RestController
@RequestMapping(value = "/mining", produces = {APPLICATION_JSON_VALUE})
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
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /mining/histogram", "Executing histogram...");

        String query = gson.toJson(queryList);
        String url = queryExaremeUrl + "/" + "MULTIPLE_HISTOGRAMS";

        try {
            StringBuilder results = new StringBuilder();
            int code = HTTPUtil.sendPost(url, query, results);

            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /mining/histogram", "Executed histogram with result :" + results.toString());
            return ResponseEntity.ok(gson.toJson(results.toString()));
        } catch (IOException e) {
            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /mining/histogram", "Histogram algorithm was not found");
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Create a descriptive statistic on Exareme", response = String.class)
    @RequestMapping(value = "/descriptive_stats", method = RequestMethod.POST)
    public ResponseEntity runExaremeDescriptiveStats(@RequestBody List<HashMap<String, String>> queryList) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats", "Executing Exareme descriptive stats...");

        String query = gson.toJson(queryList);
        String url = queryExaremeUrl + "/" + "DESCRIPTIVE_STATS";

        try {
            StringBuilder results = new StringBuilder();
            int code = HTTPUtil.sendPost(url, query, results);
            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats", "Executed descriptive stats with result : " + results.toString());
            return ResponseEntity.ok(gson.toJson(results.toString()));
        } catch (IOException e) {
            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats", "Descriptive stats algorithm was not found");
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Create a descriptive statistic on Exareme", response = String.class)
    @RequestMapping(value = "/descriptive_stats_v2", method = RequestMethod.POST)
    public ResponseEntity runExaremeDescriptiveStatsV2(@RequestBody List<HashMap<String, String>> queryList) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats_v2", "Executing an Exareme descriptive stats v2");

        String query = gson.toJson(queryList);
        String url = queryExaremeUrl + "/" + "DESCRIPTIVE_STATS_v2";

        try {
            StringBuilder results = new StringBuilder();
            int code = HTTPUtil.sendPost(url, query, results);

            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats_v2", "Successfully executed descriptive stats v2 with results : " + results.toString());
            return ResponseEntity.ok(gson.toJson(results.toString()));
        } catch (IOException e) {
            Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/descriptive_stats_v2", "Descriptive stats v2 algorithm was not found");
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Check if a formula is valid", response = String.class)
    @RequestMapping(value = "/checkFormula", method = RequestMethod.POST)
    public ResponseEntity checkFormulaValidity(String formula) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(POST) /experiments/checkFormula", "Executing  checkFormula ...");

        return ResponseEntity.ok("");
    }
}
