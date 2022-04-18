package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class MIPEngineAlgorithmRequestDTO {
    @SerializedName("request_id")
    private String request_id;
    @SerializedName("inputdata")
    private InputData inputdata;
    @SerializedName("parameters")
    private HashMap<String, Object> parameters;

    public MIPEngineAlgorithmRequestDTO(UUID experimentUUID, List<ExaremeAlgorithmRequestParamDTO> exaremeAlgorithmRequestParamDTOs) {
        this.request_id = experimentUUID.toString();
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
                    inputData.setData_model(parameter.getValue());
                    break;
                case "filter":
                    if (!parameter.getValue().equals(""))
                        rules.add(JsonConverters.convertJsonStringToObject(parameter.getValue(), MIPEngineAlgorithmRequestDTO.Filter.class));
                    break;
                default:
                    mipEngineParameters.put(parameter.getName(), convertStringToMultipleValues(parameter.getValue()));
            }
        });
        MIPEngineAlgorithmRequestDTO.Filter filter = new MIPEngineAlgorithmRequestDTO.Filter("AND", rules);
        inputData.setFilters(filter);
        this.inputdata = inputData;
        this.parameters = mipEngineParameters;
    }

    private static Object convertStringToMultipleValues(String str) {
        String[] stringValues = str.split(",");
        if (stringValues.length == 0)
            return "";

        if (stringValues.length == 1)
            return convertStringToNumeric(stringValues[0]);

        List<Object> values = new ArrayList<>();
        for (String value : stringValues) {
            values.add(convertStringToNumeric(value));
        }
        return values;
    }

    private static Object convertStringToNumeric(String str) {
        if (isInteger(str))
            return Integer.parseInt(str);
        else if (isFloat(str))
            return Double.parseDouble(str);
        else
            return str;
    }

    private static boolean isFloat(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }


    @Data
    @AllArgsConstructor
    public static class InputData {
        @SerializedName("data_model")
        private String data_model;
        @SerializedName("datasets")
        private List<String> datasets;
        @SerializedName("filters")
        private Filter filters;
        @SerializedName("x")
        private List<String> x;
        @SerializedName("y")
        private List<String> y;

        public InputData() {

        }
    }

    @Data
    @AllArgsConstructor
    public static class Filter {
        @SerializedName("condition")
        private String condition;

        @SerializedName("rules")
        private List<Object> rules;
    }
}
