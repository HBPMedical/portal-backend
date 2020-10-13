/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.springframework.validation.annotation.Validated;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "`article`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@Validated
public class Article {

    @Id
    private String slug = null;

    @NotNull
    @Size(min = 1, max = 255)
    private String title;

    private String status = null;

    @Column(columnDefinition = "text", name = "abstract")
    private String abstractText = null;

    @Column(columnDefinition = "text")
    private String content = null;

    private Date publishedAt = null;

    private Date createdAt = null;

    private Date updatedAt = null;

    @ManyToOne
    @JoinColumn(name = "createdby_username")
    private User createdBy = null;

    @ManyToOne
    @JoinColumn(name = "updatedby_username")
    private User updatedBy = null;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<Tag> tags = new LinkedList<>();


    public Article() {
        /*
         *  Empty constructor is needed by Hibernate
         */
        title = "";
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }


    @JsonProperty("abstract")
    public String getAbstract() {
        return abstractText;
    }

    public void setAbstract(String abstractText) {
        this.abstractText = abstractText;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }


    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }


    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }


    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }


    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }


    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

}
