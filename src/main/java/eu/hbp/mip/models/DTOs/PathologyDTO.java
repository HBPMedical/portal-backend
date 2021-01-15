package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PathologyDTO {

    @SerializedName("code")
    private String code;

    @SerializedName("label")
    private String label;

    @SerializedName("metadataHierarchy")
    private Object metadataHierarchy;

    @SerializedName("datasets")
    private List<PathologyDatasetDTO> datasets;

    @Getter
    @Setter
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
