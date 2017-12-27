/*
 * Created by mirco on 18.01.16.
 */

package eu.hbp.mip.controllers;

import eu.hbp.mip.model.GeneralStats;
import eu.hbp.mip.repositories.ArticleRepository;
import eu.hbp.mip.repositories.UserRepository;
import eu.hbp.mip.utils.DataUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(StatsApi.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    @Qualifier("dataUtil")
    private DataUtil dataUtil;


    @ApiOperation(value = "Get general statistics", response = GeneralStats.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<GeneralStats> getGeneralStatistics()  {
        LOGGER.info("Get statistics (count on users, articles and variables)");

        GeneralStats stats = new GeneralStats();

        stats.setUsers(userRepository.count());
        stats.setArticles(articleRepository.count());
        stats.setVariables(dataUtil.countVariables());

        return ResponseEntity.ok(stats);
    }

}
