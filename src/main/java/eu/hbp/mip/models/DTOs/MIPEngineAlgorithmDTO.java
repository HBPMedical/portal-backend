package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Hashtable;
import java.util.List;

@Data
@AllArgsConstructor
public class MIPEngineAlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private Hashtable<String, MIPEngineAlgorithmParameterDTO> parameters;

    @SerializedName("crossvalidation")
    private String crossvalidation;

    @SerializedName("inputdata")
    private MIPEngineAlgorithmInputdataDTO inputdata;

    @Data
    @AllArgsConstructor
    public static class MIPEngineAlgorithmParameterDTO {

        @SerializedName("label")
        private String label;

        @SerializedName("notblank")
        private String notblank;

        @SerializedName("multiple")
        private String multiple;

        @SerializedName("types")
        private String type;

        @SerializedName("desc")
        private String desc;

        @SerializedName("min")
        private String min;

        @SerializedName("max")
        private String max;

        @SerializedName("default_value")
        private String default_value;

        @SerializedName("enums")
        private List<String> enums;
    }

    @Data
    @AllArgsConstructor
    public static class MIPEngineAlgorithmInputdataDTO {
        @SerializedName("x")
        private MIPEngineAlgorithmInputDataDetailDTO x;

        @SerializedName("y")
        private MIPEngineAlgorithmInputDataDetailDTO y;

        @SerializedName("pathology")
        private MIPEngineAlgorithmInputDataDetailDTO pathology;

        @SerializedName("datasets")
        private MIPEngineAlgorithmInputDataDetailDTO datasets;

        @SerializedName("filter")
        private MIPEngineAlgorithmInputDataDetailDTO filter;
    }

    @Data
    @AllArgsConstructor
    public static class MIPEngineAlgorithmInputDataDetailDTO {

        @SerializedName("stattypes")
        private List<String> stattypes;

        @SerializedName("label")
        private String label;

        @SerializedName("notblank")
        private String notblank;

        @SerializedName("enumslen")
        private Integer enumslen;

        @SerializedName("multiple")
        private String multiple;

        @SerializedName("types")
        private List<String> types;

        @SerializedName("desc")
        private String desc;
    }
}
