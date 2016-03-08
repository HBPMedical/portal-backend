/**
 * Created by mirco on 04.12.15.
 */

package org.hbp.mip.controllers;


import io.swagger.annotations.*;
import org.hbp.mip.MIPApplication;
import org.hbp.mip.model.Article;
import org.hbp.mip.model.User;
import org.hbp.mip.utils.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/articles", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/articles", description = "the articles API")
public class ArticlesApi {

    @Autowired
    MIPApplication mipApplication;

    @ApiOperation(value = "Get articles", response = Article.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success") })
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List> getArticles(
            @ApiParam(value = "Only ask own articles") @RequestParam(value = "own", required = false) Boolean own,
            @ApiParam(value = "Only ask results matching status", allowableValues = "{values=[draft, published, closed]}") @RequestParam(value = "status", required = false) String status,
            @ApiParam(value = "Only ask articles from own team") @RequestParam(value = "team", required = false) Boolean team
    ) {

        User user = mipApplication.getUser();

        String queryString = "SELECT a FROM Article a, User u WHERE a.createdBy=u.id";
        if(status != null)
        {
            queryString += " AND status= :status";
        }
        if(own != null && own)
        {
            queryString += " AND u.username= :username";
        }
        else
        {
            if(team != null && team)
            {
                queryString += " AND u.team= :team";
            }
        }

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        List articles = new LinkedList<>();
        try {
            session.beginTransaction();
            Query query = session.createQuery(queryString);
            if (status != null) {
                query.setString("status", status);
            }
            if (own != null && own) {
                query.setString("username", user.getUsername());
            } else {
                if (team != null && team) {
                    query.setString("team", user.getTeam());
                }
            }
            articles = query.list();
            session.getTransaction().commit();
        } catch (Exception e)
        {
            if(session.getTransaction() != null)
            {
                session.getTransaction().rollback();
            }
        }


        return ResponseEntity.ok(articles);
    }


    @ApiOperation(value = "Create an article", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Article created") })
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> addAnArticle(
            @RequestBody @ApiParam(value = "Article to create", required = true) Article article
    ) {

        User user = mipApplication.getUser();

        article.setCreatedAt(new Date());
        if (article.getStatus().equals("published")) {
            article.setPublishedAt(new Date());
        }
        article.setSlug(article.getTitle().toLowerCase().replaceAll(" ","_"));
        article.setCreatedBy(user);

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try{
            session.beginTransaction();
            session.save(article);
            session.getTransaction().commit();
        } catch (Exception e)
        {
            if(session.getTransaction() != null)
            {
                session.getTransaction().rollback();
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Get an article", response = Article.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Found"), @ApiResponse(code = 404, message = "Not found") })
    @RequestMapping(value = "/{slug}", method = RequestMethod.GET)
    public ResponseEntity<Article> getAnArticle(
            @ApiParam(value = "slug", required = true) @PathVariable("slug") String slug
    ) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Article article = null;
        try{
            session.beginTransaction();
            Query query = session.createQuery("FROM Article WHERE slug= :slug");
            query.setString("slug", slug);
            article = (Article) query.uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e)
        {
            if(session.getTransaction() != null)
            {
                session.getTransaction().rollback();
            }
        }

        return ResponseEntity.ok(article);
    }


    @ApiOperation(value = "Update an article", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Article updated") })
    @RequestMapping(value = "/{slug}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnArticle(
            @ApiParam(value = "slug", required = true) @PathVariable("slug") String slug,
            @RequestBody @ApiParam(value = "Article to update", required = true) Article article
    ) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try{
            session.beginTransaction();
            session.update(article);
            session.getTransaction().commit();
        } catch (Exception e)
        {
            if(session.getTransaction() != null)
            {
                session.getTransaction().rollback();
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Delete an article", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Article deleted") })
    @RequestMapping(value = "/{slug}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAnArticle(
            @ApiParam(value = "slug", required = true) @PathVariable("slug") String slug
    ) {

        // TODO : Implement delete method

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
