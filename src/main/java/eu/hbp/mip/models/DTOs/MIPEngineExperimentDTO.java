package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Hashtable;
import java.util.List;

@Getter
@Setter
public class MIPEngineExperimentDTO {
    @SerializedName("inputdata")
    private InputData inputdata;
    @SerializedName("parameters")
    private Hashtable<String, Object> parameters;

    @Getter
    @Setter
    public static class InputData {
        @SerializedName("pathology")
        private String pathology;
        @SerializedName("datasets")
        private List<String> datasets;
        @SerializedName("filters")
        private Filter filters;
        @SerializedName("x")
        private List<String> x;
        @SerializedName("y")
        private List<String> y;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Filter
    {
        @SerializedName("condition")
        private String condition;

        @SerializedName("rules")
        private List<Object> rules;

        @SerializedName("valid")
        private boolean valid;
    }
}
