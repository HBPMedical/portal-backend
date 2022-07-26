package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PathologyDTO {

    @SerializedName("code")
    private String code;

    @SerializedName("version")
    private String version;


    @SerializedName("label")
    private String label;

    @SerializedName("metadataHierarchy")
    private Object metadataHierarchy;

    @SerializedName("datasets")
    private List<PathologyDatasetDTO> datasets;

    public PathologyDTO(){

    }
    @Data
    @AllArgsConstructor
    public static class PathologyDatasetDTO {
        @SerializedName("code")
        private String code;

        @SerializedName("label")
        private String label;

        public String toString() {
            return code;
        }
    }

    public String toString() {
        return code;
    }
}
