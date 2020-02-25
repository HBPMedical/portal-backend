package eu.hbp.mip.model;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import org.hibernate.annotations.Cascade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;

/**
 * Created by habfast on 21/04/16.
 */
@Entity
@Table(name = "`experiment`")
public class Experiment {

    private static final Gson gson = new Gson();

    @Id
    @Column(columnDefinition = "uuid")
    @org.hibernate.annotations.Type(type="pg-uuid")
    @Expose
    private UUID uuid;

    @Column(columnDefinition="TEXT")
    @Expose
    private String name;

    @Expose
    @ManyToOne
    @JoinColumn(name = "createdby_username")
    private User createdBy;

    @ManyToOne
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @Expose
    private Model model;

    @Column(columnDefinition="TEXT")
    @Expose
    private String algorithms;

    @Column(columnDefinition="TEXT")
    private String validations;

    @Column(columnDefinition="TEXT")
    @Expose
    private String workflowHistoryId;

    @Column(columnDefinition="TEXT")
    @Expose
    private String workflowStatus;

    @Column(columnDefinition="TEXT")
    @Expose
    private String result;

    @Expose
    private Date created = new Date();

    @Expose
    private Date finished;

    @Expose
    private boolean hasError = false;

    @Expose
    private boolean hasServerError = false;

    @Expose
    private boolean shared = false;

    // whether or not the experiment's result have been resultsViewed by its owner
    @Expose
    private boolean resultsViewed = false;

    public Experiment() {
        /*
        *  Empty constructor is needed by Hibernate
        */
    }


    public JsonObject jsonify() {
        JsonObject exp = gson.toJsonTree(this).getAsJsonObject();
        JsonParser parser = new JsonParser();

        if (this.algorithms != null)
        {
            exp.remove("algorithms");
            JsonArray jsonAlgorithms = parser.parse(this.algorithms).getAsJsonArray();
            exp.add("algorithms", jsonAlgorithms);
        }

        if (this.validations != null)
        {
            exp.remove("validations");
            JsonArray jsonValidations = parser.parse(this.validations).getAsJsonArray();
            exp.add("validations", jsonValidations);
        }

        if (this.result != null && !this.hasServerError) {
            exp.remove("result");

            JsonElement jsonResult = parser.parse(this.result);
            exp.add("result", jsonResult);

        }

        return exp;
    }

    public String getValidations() {
        return validations;
    }

    public void setValidations(String validations) {
        this.validations = validations;
    }

    public String getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(String algorithms) {
        this.algorithms = algorithms;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getWorkflowHistoryId() {
        return workflowHistoryId;
    }

    public void setWorkflowHistoryId(String workflowHistoryId) {
        this.workflowHistoryId = workflowHistoryId;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isResultsViewed() {
        return resultsViewed;
    }

    public void setResultsViewed(boolean resultsViewed) {
        this.resultsViewed = resultsViewed;
    }

    public boolean isHasServerError() {
        return hasServerError;
    }

    public void setHasServerError(boolean hasServerError) {
        this.hasServerError = hasServerError;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
