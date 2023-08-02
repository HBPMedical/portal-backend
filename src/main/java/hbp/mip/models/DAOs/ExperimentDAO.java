package hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


@Entity
@Data
@AllArgsConstructor
@Table(name = "`experiment`")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperimentDAO {

    private static final Gson gson = new Gson();

    @Expose
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
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

    @Override
    public String toString() {
        String finishedDT = "";
        if (this.finished != null)
            finishedDT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(this.finished);

        return "ExperimentDAO(uuid=" + this.uuid +
                ", name=" + this.name +
                ", createdBy=" + this.createdBy +
                ", workflowHistoryId=" + this.workflowHistoryId +
                ", status=" + this.status +
                ", result=" + this.result +
                ", finished=" + finishedDT +
                ", algorithm=" + this.algorithm +
                ", algorithmId=" + this.algorithmId +
                ", created=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(this.created) +
                ", updated=" + this.updated +
                ", shared=" + this.shared +
                ", viewed=" + this.viewed + ")";
    }

    public ExperimentDAO() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }

}
