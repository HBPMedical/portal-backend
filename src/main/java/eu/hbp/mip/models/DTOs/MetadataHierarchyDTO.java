package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class MetadataHierarchyDTO {

    @SerializedName("variables")
    private List<CommonDataElement> variables;

    @SerializedName("code")
    private String code;

    @SerializedName("groups")
    private Object groups;

    @SerializedName("label")
    private String label;
    @Data
    @AllArgsConstructor
    public static class CommonDataElement {
        @SerializedName("isCategorical")
        private Boolean isCategorical;

        @SerializedName("code")
        private String code;

        @SerializedName("sql_type")
        private String sql_type;

        @SerializedName("description")
        private String description;

        @SerializedName("enumerations")
        private Object enumerations;

        @SerializedName("label")
        private String label;

        @SerializedName("units")
        private String units;

        @SerializedName("type")
        private String type;

        @SerializedName("methodology")
        private String methodology;

    }
}
