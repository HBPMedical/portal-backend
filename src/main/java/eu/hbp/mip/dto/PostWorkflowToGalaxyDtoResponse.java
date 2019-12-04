/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.dto;

import com.google.gson.annotations.SerializedName;

public class PostWorkflowToGalaxyDtoResponse {


    @SerializedName("update_time")
    String updateTime;
    String uuid;
    @SerializedName("history_id")
    String historyId;
    String stake;
    @SerializedName("workflow_id")
    String workflowId;
    @SerializedName("model_class")
    String modelClass;
    String id;

    public PostWorkflowToGalaxyDtoResponse() {
    }

    public PostWorkflowToGalaxyDtoResponse(String updateTime, String uuid, String historyId, String stake, String workflowId, String modelClass, String id) {
        this.updateTime = updateTime;
        this.uuid = uuid;
        this.historyId = historyId;
        this.stake = stake;
        this.workflowId = workflowId;
        this.modelClass = modelClass;
        this.id = id;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getStake() {
        return stake;
    }

    public void setStake(String stake) {
        this.stake = stake;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getModelClass() {
        return modelClass;
    }

    public void setModelClass(String modelClass) {
        this.modelClass = modelClass;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
