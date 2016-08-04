/**
 * Created by mirco on 18.01.16.
 */

package eu.hbp.mip.controllers;

import eu.hbp.mip.repositories.VariableRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import eu.hbp.mip.model.GeneralStats;
import eu.hbp.mip.repositories.ArticleRepository;
import eu.hbp.mip.repositories.UserRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/stats", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/stats", description = "the stats API")
public class StatsApi {

    private static final Logger LOGGER = Logger.getLogger(StatsApi.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    VariableRepository variableRepository;

    @ApiOperation(value = "Get general statistics", response = GeneralStats.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Found"), @ApiResponse(code = 404, message = "Not found") })
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<GeneralStats> getGeneralStatistics()  {
        LOGGER.info("Get statistics (count on users, articles and variables)");

        GeneralStats stats = new GeneralStats();

        stats.setUsers(userRepository.count());
        stats.setArticles(articleRepository.count());
        stats.setVariables(variableRepository.count());

        return ResponseEntity.ok(stats);
    }

}