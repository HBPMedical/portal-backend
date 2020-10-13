/*
 * Created by mirco on 18.01.16.
 */

package eu.hbp.mip.controllers;

import eu.hbp.mip.model.GeneralStats;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.repositories.ArticleRepository;
import eu.hbp.mip.repositories.UserRepository;
import eu.hbp.mip.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private ArticleRepository articleRepository;


    @ApiOperation(value = "Get general statistics", response = GeneralStats.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<GeneralStats> getGeneralStatistics() {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /stats", "Loading general statistics");

        GeneralStats stats = new GeneralStats();

        stats.setUsers(userRepository.count());
        stats.setArticles(articleRepository.count());

        Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /stats", "Loaded " + userRepository.count() + " user statistics and " + articleRepository.count() + " artcle statistics.");
        return ResponseEntity.ok(stats);
    }

}
