/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.controllers;


import com.github.slugify.Slugify;
import eu.hbp.mip.model.Article;
import eu.hbp.mip.model.User;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.repositories.ArticleRepository;
import eu.hbp.mip.utils.Logging;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/articles", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/articles", description = "the articles API")
public class ArticlesApi {

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private ArticleRepository articleRepository;

    @ApiOperation(value = "Get articles", response = Article.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Iterable> getArticles(
            @ApiParam(value = "Only ask own articles") @RequestParam(value = "own", required = false) Boolean own,
            @ApiParam(value = "Only ask results matching status", allowableValues = "draft, published") @RequestParam(value = "status", required = false) String status
    ) {
        User user = userInfo.getUser();
        Iterable<Article> articles;
        Logging.LogUserAction(user.getUsername(), "(GET) /articles", "Loading articles...");

        if (own != null && own) {
            articles = articleRepository.findByCreatedBy(user);
        } else {
            articles = articleRepository.findByStatusOrCreatedBy("published", user);
        }
        int articlesSize = 0;
        if (status != null) {
            for (Iterator<Article> i = articles.iterator(); i.hasNext(); ) {
                Article a = i.next();
                if (!status.equals(a.getStatus())) {
                    i.remove();
                }
                articlesSize++;
            }
        }
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /articles", "Successfully Loaded " + articlesSize + " articles");

        return ResponseEntity.ok(articles);
    }


    @ApiOperation(value = "Create an article")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Article created")})
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> addAnArticle(
            @RequestBody @ApiParam(value = "Article to create", required = true) @Valid Article article
    ) {

        User user = userInfo.getUser();
        Logging.LogUserAction(user.getUsername(), "(POST) /articles", "Creating article...");

        article.setCreatedAt(new Date());
        if ("published".equals(article.getStatus())) {
            article.setPublishedAt(new Date());
        }
        article.setCreatedBy(user);

        long count = 1;
        for (int i = 1; count > 0; i++) {
            count = articleRepository.countByTitle(article.getTitle());

            if (count > 0) {
                String title = article.getTitle();
                if (i > 1) {
                    title = title.substring(0, title.length() - 4);
                }
                article.setTitle(title + " (" + i + ")");
            }
        }

        String slug;
        try {
            slug = new Slugify().slugify(article.getTitle());
        } catch (IOException e) {
            slug = "";
            //LOGGER.trace("Cannot slugify title", e);
        }

        boolean alreadyExists = true;
        for (int i = 1; alreadyExists; i++) {
            alreadyExists = articleRepository.exists(slug);
            if (alreadyExists) {
                if (i > 1) {
                    slug = slug.substring(0, slug.length() - 2);
                }
                slug += "-" + i;
            }
            article.setSlug(slug);
        }
        articleRepository.save(article);

        Logging.LogUserAction(user.getUsername(), "(POST) /articles", "Successfully created article with id : " + article.getSlug());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @ApiOperation(value = "Get an article", response = Article.class)
    @RequestMapping(value = "/{slug}", method = RequestMethod.GET)
    public ResponseEntity<Article> getAnArticle(
            @ApiParam(value = "slug", required = true) @PathVariable("slug") String slug
    ) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /articles/{slug}", "Loading article with id : " + slug);

        User user = userInfo.getUser();
        Article article;
        article = articleRepository.findOne(slug);

        if (article == null) {
            //LOGGER.warn("Cannot find article : " + slug);
            Logging.LogUserAction(userInfo.getUser().getUsername(), "(GET) /articles/{slug}", "Article not found");
            return ResponseEntity.badRequest().body(null);
        }

        if (!"published".equals(article.getStatus()) && !article.getCreatedBy().getUsername().equals(user.getUsername())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Logging.LogUserAction(user.getUsername(), "(GET) /articles/{slug}", "Successfully Loaded article with id : " + slug);

        return ResponseEntity.ok(article);
    }


    @ApiOperation(value = "Update an article")
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Article updated")})
    @RequestMapping(value = "/{slug}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnArticle(
            @ApiParam(value = "slug", required = true) @PathVariable("slug") String slug,
            @RequestBody @ApiParam(value = "Article to update", required = true) @Valid Article article
    ) {
        Logging.LogUserAction(userInfo.getUser().getUsername(), "(PUT) /articles/{slug}", "Updating article with id : " + slug);

        User user = userInfo.getUser();

        String author = articleRepository.findOne(slug).getCreatedBy().getUsername();

        if (!user.getUsername().equals(author)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String oldTitle = articleRepository.findOne(slug).getTitle();

        String newTitle = article.getTitle();

        if (!newTitle.equals(oldTitle)) {
            long count = 1;
            for (int i = 1; count > 0 && !newTitle.equals(oldTitle); i++) {
                newTitle = article.getTitle();
                count = articleRepository.countByTitle(newTitle);
                if (count > 0 && !newTitle.equals(oldTitle)) {
                    if (i > 1) {
                        newTitle = newTitle.substring(0, newTitle.length() - 4);
                    }
                    article.setTitle(newTitle + " (" + i + ")");
                }
            }
        }

        articleRepository.save(article);

        Logging.LogUserAction(userInfo.getUser().getUsername(), "(PUT) /articles/{slug}", "Successfully pdated article with id : " + slug);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
