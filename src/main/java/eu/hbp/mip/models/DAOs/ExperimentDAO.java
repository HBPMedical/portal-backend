package eu.hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import eu.hbp.mip.models.DTOs.AlgorithmDTO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.utils.JsonConverters;
import io.swagger.annotations.ApiModel;
import org.svenson.JSONParser;

import javax.persistence.*;
import java.util.*;

/**
 * Created by habfast on 21/04/16.
 */
@Entity
@Table(name = "`experiment`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperimentDAO {

    private static final Gson gson = new Gson();

    @Expose
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @org.hibernate.annotations.Type(type = "pg-uuid")
    private UUID uuid;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String name;

    @Expose
    @ManyToOne
    @JoinColumn(name = "created_by_username",columnDefinition = "CHARACTER VARYING")
    private UserDAO createdBy;

    @Expose
    @Column(name="workflow_history_id", columnDefinition = "TEXT")
    private String workflowHistoryId;

    @Expose
    @Column(columnDefinition = "TEXT")
    private Status status;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String result;

    @Expose
    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Date finished;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String algorithmDetails;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String algorithm;

    @Expose
    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Date created = new Date();
    
    @Expose
    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Date updated;

    @Expose
    @Column(columnDefinition = "BOOLEAN")
    private boolean shared = false;

    // whether or not the experiment's result have been viewed by its owner
    @Expose
    @Column(columnDefinition = "BOOLEAN")
    private boolean viewed = false;

    public enum Status {
        error,
        pending,
        success
    }

    public enum Type {
        WARNING("text/plain+warning"),
        USER_WARNING("text/plain+user_error"),
        HIGHCHARTS("application/vnd.highcharts+json"),
        JSON("application/json"),
        JSONBTREE("application/binary-tree+json"),
        PFA("application/pfa+json"),
        JSONDATA("application/vnd.dataresource+json"),
        HTML("text/html"),
        TEXT("text/plain");

        private String type;

        //Constructor to initialize the instance variable
        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
    }

    public ExperimentDAO() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }

    public ExperimentDTO convertToDTO(boolean includeResult)
    {
        ExperimentDTO experimentDTO = new ExperimentDTO();
        experimentDTO.setAlgorithmDetails(JsonConverters.convertJsonStringToObject(this.algorithmDetails, AlgorithmDTO.class));
        experimentDTO.setCreated(this.created);
        experimentDTO.setUpdated(this.updated);
        experimentDTO.setFinished(this.finished);
        experimentDTO.setCreatedBy(this.createdBy.getUsername());
        experimentDTO.setName(this.name);
        if(includeResult){
            experimentDTO.setResult(JsonConverters.convertJsonStringToObject(String.valueOf(this.result), new ArrayList<ExperimentDTO.ResultDTO>().getClass()));
        }
        experimentDTO.setStatus(this.status);
        experimentDTO.setShared(this.shared);
        experimentDTO.setUuid(this.uuid);
        experimentDTO.setViewed(this.viewed);
        return experimentDTO;
    }

    public String getAlgorithmDetails() {
        return algorithmDetails;
    }

    public void setAlgorithmDetails(String algorithmDetails) {
        this.algorithmDetails = algorithmDetails;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getWorkflowHistoryId() {
        return workflowHistoryId;
    }

    public void setWorkflowHistoryId(String workflowHistoryId) {
        this.workflowHistoryId = workflowHistoryId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
    
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
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

    public UserDAO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserDAO createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
