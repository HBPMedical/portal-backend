/*
 * Created by mirco on 18.01.16.
 */

package eu.hbp.mip.controllers;

import eu.hbp.mip.model.GeneralStats;
import eu.hbp.mip.repositories.ArticleRepository;
import eu.hbp.mip.repositories.UserRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/stats", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/stats", description = "the stats API")
public class StatsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsApi.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;


    @ApiOperation(value = "Get general statistics", response = GeneralStats.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<GeneralStats> getGeneralStatistics()  {
        LOGGER.info("Get statistics (count on users, articles and variables)");

        GeneralStats stats = new GeneralStats();

        stats.setUsers(userRepository.count());
        stats.setArticles(articleRepository.count());

        return ResponseEntity.ok(stats);
    }

}
