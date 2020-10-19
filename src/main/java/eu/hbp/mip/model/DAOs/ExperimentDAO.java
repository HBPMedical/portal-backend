package eu.hbp.mip.model.DAOs;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import eu.hbp.mip.model.DTOs.AlgorithmDTO;
import eu.hbp.mip.model.DTOs.ExperimentDTO;
import eu.hbp.mip.model.User;
import eu.hbp.mip.utils.JsonConverters;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Created by habfast on 21/04/16.
 */
@Entity
@Table(name = "`experiment`")
public class ExperimentDAO {

    private static final Gson gson = new Gson();

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    @org.hibernate.annotations.Type(type = "pg-uuid")
    @Expose
    private UUID uuid;

    @Column(columnDefinition = "TEXT")
    @Expose
    private String name;

    @Expose
    @ManyToOne
    @JoinColumn(name = "created_by_username")
    private User createdBy;

    @Column(name="workflow_history_id", columnDefinition = "TEXT")
    @Expose
    private String workflowHistoryId;

    @Column(columnDefinition = "TEXT")
    @Expose
    private Status status;

    @Column(columnDefinition = "TEXT")
    @Expose
    private String result;

    @Expose
    private Date finished;

    @Expose
    private String algorithm;

    @Expose
    private Date created = new Date();

    @Expose
    private boolean shared = false;

    // whether or not the experiment's result have been viewed by its owner
    @Expose
    private boolean viewed = false;

    public enum Status {
        error,
        pending,
        success
    }

    public enum MimeTypes {
        ERROR("text/plain+error"),
        WARNING("text/plain+warning"),
        USER_WARNING("text/plain+user_error"),
        HIGHCHARTS("application/vnd.highcharts+json"),
        JSON("application/json"),
        JSONBTREE("application/binary-tree+json"),
        PFA("application/pfa+json"),
        JSONDATA("application/vnd.dataresource+json"),
        HTML("text/html"),
        TEXT("text/plain");

        private String types;

        //Constructor to initialize the instance variable
        MimeTypes(String types) {
            this.types = types;
        }

        public String getTypes() {
            return this.types;
        }
    }

    public ExperimentDAO() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }

    public ExperimentDTO convertToDTO()
    {
        ExperimentDTO experimentDTO = new ExperimentDTO();
        experimentDTO.setAlgorithm(JsonConverters.convertJsonStringToObject(this.algorithm, AlgorithmDTO.class));
        experimentDTO.setCreated(this.created);
        experimentDTO.setCreatedBy(this.createdBy.getUsername());
        experimentDTO.setName(this.name);
        experimentDTO.setResult(JsonConverters.convertJsonStringToObject(this.result, ExperimentDTO.ResultDTO.class));
        experimentDTO.setShared(this.shared);
        experimentDTO.setUuid(this.uuid.toString());
        experimentDTO.setViewed(this.viewed);
        return experimentDTO;
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
