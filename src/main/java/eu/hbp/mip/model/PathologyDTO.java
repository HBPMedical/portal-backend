package eu.hbp.mip.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PathologyDTO {

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Object getMetadataHierarchy() {
        return metadataHierarchy;
    }

    public void setMetadataHierarchy(Object metadataHierarchy) {
        this.metadataHierarchy = metadataHierarchy;
    }

    public List<PathologyDatasetDTO> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<PathologyDatasetDTO> datasets) {
        this.datasets = datasets;
    }

    @SerializedName("code")
    private String code;

    @SerializedName("label")
    private String label;

    @SerializedName("metadataHierarchy")
    private Object metadataHierarchy;

    @SerializedName("datasets")
    private List<PathologyDatasetDTO> datasets;

    public static class PathologyDatasetDTO {
        @SerializedName("code")
        private String code;

        @SerializedName("label")
        private String label;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String toString() {
            return code;
        }
    }

    public String toString() {
        return code;
    }

}
