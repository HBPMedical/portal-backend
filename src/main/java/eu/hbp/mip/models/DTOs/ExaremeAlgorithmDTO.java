package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ExaremeAlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private List<ExaremeAlgorithmRequestParamDTO> parameters;


    @Getter
    @Setter
    @AllArgsConstructor
    static class Rule
    {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        @SerializedName("operator")
        private String operator;

        @SerializedName("value")
        private Object value;
    }
    public MIPEngineAlgorithmRequestDTO convertToMIPEngineBody()
    {
        MIPEngineAlgorithmRequestDTO mipEngineAlgorithmRequestDTO = new MIPEngineAlgorithmRequestDTO();
        MIPEngineAlgorithmRequestDTO.InputData inputData = new MIPEngineAlgorithmRequestDTO.InputData();
        HashMap<String, Object> mipEngineParameters = new HashMap<>();

        List<Object> rules = new ArrayList<>();
        this.parameters.forEach(parameter -> {

            switch (parameter.getName()) {
                case "x":
                    List<String> x = Arrays.asList(parameter.getValue().split(","));
                    x.forEach(column -> rules.add(new Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                    inputData.setX(x);
                    break;
                case "y":
                    List<String> y = Arrays.asList(parameter.getValue().split(","));
                    y.forEach(column -> rules.add(new Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                    inputData.setY(y);
                    break;
                case "dataset":
                    List<String> datasets = Arrays.asList(parameter.getValue().split(","));
                    rules.add(new Rule("dataset", "string", "in", datasets));
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
        MIPEngineAlgorithmRequestDTO.Filter filter = new MIPEngineAlgorithmRequestDTO.Filter("AND", rules, true);
        inputData.setFilters(filter);
        mipEngineAlgorithmRequestDTO.setInputdata(inputData);
        mipEngineAlgorithmRequestDTO.setParameters(mipEngineParameters);
        return mipEngineAlgorithmRequestDTO;
    }
}
