/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalaxyWorkflowResult {

    @SerializedName("history_content_type")
    private String historyContentType;
    @SerializedName("update_time")
    private String updateTime;
    @SerializedName("name")
    private String name;
    @SerializedName("extension")
    private String extension;
    @SerializedName("type_id")
    private String typeId;
    @SerializedName("deleted")
    private Boolean deleted;
    @SerializedName("history_id")
    private String historyId;
    @SerializedName("tags")
    private List<Object> tags = null;
    @SerializedName("id")
    private String id;
    @SerializedName("visible")
    private Boolean visible;
    @SerializedName("state")
    private String state;
    @SerializedName("create_time")
    private String createTime;
    @SerializedName("hid")
    private Integer hid;
    @SerializedName("url")
    private String url;
    @SerializedName("dataset_id")
    private String datasetId;
    @SerializedName("type")
    private String type;
    @SerializedName("purged")
    private Boolean purged;

    public GalaxyWorkflowResult() {
    }

    public GalaxyWorkflowResult(String historyContentType, String updateTime, String name, String extension, String typeId, Boolean deleted, String historyId, List<Object> tags, String id, Boolean visible, String state, String createTime, Integer hid, String url, String datasetId, String type, Boolean purged) {
        this.historyContentType = historyContentType;
        this.updateTime = updateTime;
        this.name = name;
        this.extension = extension;
        this.typeId = typeId;
        this.deleted = deleted;
        this.historyId = historyId;
        this.tags = tags;
        this.id = id;
        this.visible = visible;
        this.state = state;
        this.createTime = createTime;
        this.hid = hid;
        this.url = url;
        this.datasetId = datasetId;
        this.type = type;
        this.purged = purged;
    }

    public String getHistoryContentType() {
        return historyContentType;
    }

    public void setHistoryContentType(String historyContentType) {
        this.historyContentType = historyContentType;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public List<Object> getTags() {
        return tags;
    }

    public void setTags(List<Object> tags) {
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getHid() {
        return hid;
    }

    public void setHid(Integer hid) {
        this.hid = hid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getPurged() {
        return purged;
    }

    public void setPurged(Boolean purged) {
        this.purged = purged;
    }
}

