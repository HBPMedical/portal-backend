package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Data
@AllArgsConstructor
public class MIPEngineAlgorithmRequestDTO {
    @SerializedName("inputdata")
    private InputData inputdata;
    @SerializedName("parameters")
    private HashMap<String, Object> parameters;

    public MIPEngineAlgorithmRequestDTO(List<ExaremeAlgorithmRequestParamDTO> exaremeAlgorithmRequestParamDTOs)
    {
        MIPEngineAlgorithmRequestDTO.InputData inputData = new MIPEngineAlgorithmRequestDTO.InputData();
        HashMap<String, Object> mipEngineParameters = new HashMap<>();

        List<Object> rules = new ArrayList<>();
        exaremeAlgorithmRequestParamDTOs.forEach(parameter -> {

            switch (parameter.getName()) {
                case "x":
                    List<String> x = Arrays.asList(parameter.getValue().split(","));
                    x.forEach(column -> rules.add(new ExaremeAlgorithmDTO.Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                    inputData.setX(x);
                    break;
                case "y":
                    List<String> y = Arrays.asList(parameter.getValue().split(","));
                    y.forEach(column -> rules.add(new ExaremeAlgorithmDTO.Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                    inputData.setY(y);
                    break;
                case "dataset":
                    List<String> datasets = Arrays.asList(parameter.getValue().split(","));
                    rules.add(new ExaremeAlgorithmDTO.Rule("dataset", "string", "in", datasets));
                    inputData.setDatasets(datasets);
                    break;
                case "pathology":
                    inputData.setPathology(parameter.getValue());
                    break;
                case "filter":
                    if (!parameter.getValue().equals(""))
                        rules.add(JsonConverters.convertJsonStringToObject(parameter.getValue(), MIPEngineAlgorithmRequestDTO.Filter.class));
                    break;
                default:
                    mipEngineParameters.put(parameter.getName(), Arrays.asList(parameter.getValue().split(",")));
                    break;
            }
        });
        MIPEngineAlgorithmRequestDTO.Filter filter = new MIPEngineAlgorithmRequestDTO.Filter("AND", rules);
        inputData.setFilters(filter);
        this.inputdata = inputData;
        this.parameters = mipEngineParameters;
    }


    @Data
    @AllArgsConstructor
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
        public InputData(){

        }
    }

    @Data
    @AllArgsConstructor
    public static class Filter
    {
        @SerializedName("condition")
        private String condition;

        @SerializedName("rules")
        private List<Object> rules;
    }
}
