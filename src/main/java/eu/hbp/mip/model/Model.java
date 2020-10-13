/**
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "`model`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Model {

    @Id
    @Expose
    private String slug = null;

    @Expose
    private String title = null;

    private String description = null;

    private Boolean valid = null;

    private Date createdAt = null;

    private Date updatedAt = null;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    private eu.hbp.mip.model.Query query = null;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    private Dataset dataset = null;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    private Config config = null;

    @ManyToOne
    @JoinColumn(name = "createdby_username")
    private User createdBy = null;

    @ManyToOne
    @JoinColumn(name = "updatedby_username")
    private User updatedBy = null;


    public Model() {
        /*
         *  Empty constructor is needed by Hibernate
         */
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


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public eu.hbp.mip.model.Query getQuery() {
        return query;
    }

    public void setQuery(eu.hbp.mip.model.Query query) {
        this.query = query;
    }


    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }


    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }


    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
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

}
