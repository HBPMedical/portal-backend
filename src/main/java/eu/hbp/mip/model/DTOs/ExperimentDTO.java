package eu.hbp.mip.model.DTOs;

import eu.hbp.mip.model.DAOs.ExperimentDAO;

import java.util.Date;
import java.util.List;

public class ExperimentDTO {

    private String uuid;
    private String name;
    private String createdBy;
    private Date created;
    private Boolean shared;
    private Boolean viewed;
    private ExperimentDTO.ResultDTO result;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
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

    public ExperimentDTO.ResultDTO getResult() {
        return result;
    }

    public void setResult(ExperimentDTO.ResultDTO result) {
        this.result = result;
    }

    public ExperimentDAO.Status getStatus() {
        return status;
    }

    public void setStatus(ExperimentDAO.Status status) {
        this.status = status;
    }

    public static class OutputDTO {

        private String data;
        private ExperimentDAO.MimeTypes mimeTypes;

        public String getData() {
            return this.data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public ExperimentDAO.MimeTypes getMimeTypes() {
            return mimeTypes;
        }

        public void setMimeTypes(ExperimentDAO.MimeTypes mimeTypes) {
            this.mimeTypes = mimeTypes;
        }
    }

    public static class ResultDTO {
        private List<OutputDTO> result;

        public List<OutputDTO> getResult() {
            return this.result;
        }

        public void setResult(List<OutputDTO> result) {
            this.result = result;
        }
    }
}
