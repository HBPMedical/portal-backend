package eu.hbp.mip.model.DTOs;

import eu.hbp.mip.model.DAOs.ExperimentDAO;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class ExperimentDTO {

    private UUID uuid;
    private String name;
    private String createdBy;
    private Date created;
    private Date updated;
    private Date finished;
    private Boolean shared;
    private Boolean viewed;
    private Map result;
    private ExperimentDAO.Status status;

    private String algorithm;
    private AlgorithmDTO algorithmDetails;

    public ExperimentDTO() {
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public AlgorithmDTO getAlgorithmDetails() {
        return algorithmDetails;
    }

    public void setAlgorithmDetails(AlgorithmDTO algorithmDetails) {
        this.algorithmDetails = algorithmDetails;
        this.algorithm = algorithmDetails.getName();
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    
    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public Map getResult() {
        return result;
    }

    public void setResult(Map result) {
        this.result = result;
    }

    public ExperimentDAO.Status getStatus() {
        return status;
    }

    public void setStatus(ExperimentDAO.Status status) {
        this.status = status;
    }

    public static class ResultDTO {

        private Object data;
        private ExperimentDAO.Type type;

        public Object getData() {
            return this.data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public ExperimentDAO.Type getType() {
            return type;
        }

        public void setType(ExperimentDAO.Type type) {
            this.type = type;
        }
    }
}
