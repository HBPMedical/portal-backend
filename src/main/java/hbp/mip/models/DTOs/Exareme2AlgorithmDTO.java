package hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class Exareme2AlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private Hashtable<String, Exareme2AlgorithmParameterDTO> parameters;

    @SerializedName("crossvalidation")
    private String crossvalidation;

    @SerializedName("inputdata")
    private Exareme2AlgorithmInputdataDTO inputdata;

    public Optional<Hashtable<String, Exareme2AlgorithmParameterDTO>> getParameters() {
        return Optional.ofNullable(parameters);
    }
    @SerializedName("preprocessing")
    private List<Exareme2TransformerDTO> preprocessing;

    public Optional<List<Exareme2TransformerDTO>> getPreprocessing() {
        return Optional.ofNullable(preprocessing);
    }


    @Data
    @AllArgsConstructor
    public static class Exareme2AlgorithmParameterDTO {

        @SerializedName("label")
        private String label;

        @SerializedName("notblank")
        private String notblank;

        @SerializedName("multiple")
        private String multiple;

        @SerializedName("types")
        private List<String> types;

        @SerializedName("desc")
        private String desc;

        @SerializedName("min")
        private String min;

        @SerializedName("max")
        private String max;

        @SerializedName("default")
        private String default_value;

        @SerializedName("enums")
        private Exareme2AlgorithmEnumDTO enums;

        @SerializedName("dict_keys_enums")
        private Exareme2AlgorithmEnumDTO dict_keys_enums;

        @SerializedName("dict_values_enums")
        private Exareme2AlgorithmEnumDTO dict_values_enums;

        public Optional<Exareme2AlgorithmEnumDTO> getEnums() {
            return Optional.ofNullable(enums);
        }

        @Data
        @AllArgsConstructor
        public static class Exareme2AlgorithmEnumDTO {

            @SerializedName("type")
            private String type;

            @SerializedName("source")
            private List<String> source;

        }
    }

    @Data
    @AllArgsConstructor
    public static class Exareme2AlgorithmInputdataDTO {

        @SerializedName("x")
        private Exareme2AlgorithmInputDataDetailDTO x;

        @SerializedName("y")
        private Exareme2AlgorithmInputDataDetailDTO y;

        @SerializedName("data_model")
        private Exareme2AlgorithmInputDataDetailDTO data_model;

        @SerializedName("datasets")
        private Exareme2AlgorithmInputDataDetailDTO datasets;

        @SerializedName("filter")
        private Exareme2AlgorithmInputDataDetailDTO filter;

        public Optional<Exareme2AlgorithmInputDataDetailDTO> getY() {
            return Optional.ofNullable(y);
        }

        public Optional<Exareme2AlgorithmInputDataDetailDTO> getX() {
            return Optional.ofNullable(x);
        }
    }



    @Data
    @AllArgsConstructor
    public static class Exareme2AlgorithmInputDataDetailDTO {

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

    @Data
    @AllArgsConstructor
    public static class Exareme2TransformerDTO {
        @SerializedName("name")
        private String name;

        @SerializedName("desc")
        private String desc;

        @SerializedName("label")
        private String label;

        @SerializedName("parameters")
        private Hashtable<String, Exareme2AlgorithmParameterDTO> parameters;

    }
}
