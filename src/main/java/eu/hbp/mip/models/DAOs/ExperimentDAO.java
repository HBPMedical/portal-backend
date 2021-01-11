package eu.hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import eu.hbp.mip.models.DTOs.AlgorithmDTO;
import eu.hbp.mip.models.DTOs.ExperimentDTO;
import eu.hbp.mip.utils.JsonConverters;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

/**
 * Created by habfast on 21/04/16.
 */
@Entity
@Getter
@Setter
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
    @Enumerated(EnumType.STRING)
    private Status status;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String result;

    @Expose
    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Date finished;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String algorithm;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String algorithmId;

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

    public ExperimentDAO() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }

    public ExperimentDTO convertToDTO(boolean includeResult)
    {
        ExperimentDTO experimentDTO = new ExperimentDTO();
        experimentDTO.setAlgorithm(JsonConverters.convertJsonStringToObject(this.algorithm, AlgorithmDTO.class));
        experimentDTO.setCreated(this.created);
        experimentDTO.setUpdated(this.updated);
        experimentDTO.setFinished(this.finished);
        experimentDTO.setCreatedBy(this.createdBy.getUsername());
        experimentDTO.setName(this.name);
        if(includeResult){
            experimentDTO.setResult(JsonConverters.convertJsonStringToObject(String.valueOf(this.result),  new ArrayList<>().getClass()));
        }
        experimentDTO.setStatus(this.status);
        experimentDTO.setShared(this.shared);
        experimentDTO.setUuid(this.uuid);
        experimentDTO.setViewed(this.viewed);
        return experimentDTO;
    }
}
