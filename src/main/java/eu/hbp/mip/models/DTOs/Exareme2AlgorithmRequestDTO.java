package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class Exareme2AlgorithmRequestDTO {
    @SerializedName("request_id")
    private String request_id;
    @SerializedName("inputdata")
    private InputData inputdata;
    @SerializedName("parameters")
    private HashMap<String, Object> parameters;
    @SerializedName("preprocessing")
    private HashMap<String, Object> preprocessing;

    public Exareme2AlgorithmRequestDTO(UUID experimentUUID, List<ExaremeAlgorithmRequestParamDTO> exaremeAlgorithmRequestParamDTOs, List<ExaremeAlgorithmDTO.ExaremeTransformerDTO> exaremeTransformers) {
        this.request_id = experimentUUID.toString();
        Exareme2AlgorithmRequestDTO.InputData inputData = new Exareme2AlgorithmRequestDTO.InputData();
        HashMap<String, Object> exareme2Parameters = new HashMap<>();
        HashMap<String, Object> exareme2Preprocessing = new HashMap<>();

        exaremeAlgorithmRequestParamDTOs.forEach(parameter -> {

            switch (parameter.getName()) {
                case "x":
                    List<String> x = Arrays.asList(parameter.getValue().split(","));
                    inputData.setX(x);
                    break;
                case "y":
                    List<String> y = Arrays.asList(parameter.getValue().split(","));
                    inputData.setY(y);
                    break;
                case "dataset":
                    List<String> datasets = Arrays.asList(parameter.getValue().split(","));
                    inputData.setDatasets(datasets);
                    break;
                case "pathology":
                    inputData.setData_model(parameter.getValue());
                    break;
                case "filter":
                    if (parameter.getValue() != null && !parameter.getValue().equals(""))
                        inputData.setFilters(JsonConverters.convertJsonStringToObject(parameter.getValue(), Exareme2AlgorithmRequestDTO.Filter.class));
                    break;
                default:
                    exareme2Parameters.put(parameter.getName(), convertStringToMultipleValues(parameter.getValue()));
            }
        });
        if (exaremeTransformers != null) {
            exaremeTransformers.forEach(transformer -> {
                HashMap<String, Object> transformerParameterDTOs = new HashMap<>();
                for (ExaremeAlgorithmRequestParamDTO parameter : transformer.getParameters()) {
                    if (parameter.getName().equals("strategies")){
                        transformerParameterDTOs.put(parameter.getName(),
                                JsonConverters.convertJsonStringToObject(parameter.getValue(), HashMap.class)
                        );
                    }
                    else {
                        transformerParameterDTOs.put(parameter.getName(), convertStringToMultipleValues(parameter.getValue()));
                    }
                }
                exareme2Preprocessing.put(transformer.getName(), transformerParameterDTOs);
            });
        }
        this.inputdata = inputData;
        this.parameters = exareme2Parameters;
        this.preprocessing = exareme2Preprocessing;
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