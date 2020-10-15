/*
 * Created by mirco on 03.02.16.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralStats {

    private Long users = null;
    private Long articles = null;


    public GeneralStats() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }


    public Long getUsers() {
        return users;
    }

    public void setUsers(Long users) {
        this.users = users;
    }


    public Long getArticles() {
        return articles;
    }

    public void setArticles(Long articles) {
        this.articles = articles;
    }
}
