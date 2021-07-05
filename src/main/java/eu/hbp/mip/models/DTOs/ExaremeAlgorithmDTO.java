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
    private List<ExaremeAlgorithmRequestDTO> parameters;


    @Getter
    @Setter
    @AllArgsConstructor
    class Rule
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
    public MIPEngineExperimentDTO convertToMIPEngineBody()
    {
        MIPEngineExperimentDTO mipEngineExperimentDTO = new MIPEngineExperimentDTO();
        MIPEngineExperimentDTO.InputData inputData = new MIPEngineExperimentDTO.InputData();
        Hashtable<String, Object> mipEngineParameters = new Hashtable<>();

        List<Object> rules = new ArrayList<>();
        this.parameters.forEach(parameter -> {

            if(parameter.getName().equals("x")) {
                List<String> x = Arrays.asList(parameter.getValue().split(","));
                x.forEach(column -> rules.add(new Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                inputData.setX(x);
            }
            else if(parameter.getName().equals("y")) {
                List<String> y = Arrays.asList(parameter.getValue().split(","));
                y.forEach(column -> rules.add(new Rule(column, parameter.getColumnValuesSQLType(), "is_not_null", null)));
                inputData.setY(y);
            }
            else if(parameter.getName().equals("dataset")){
                List<String> datasets = Arrays.asList(parameter.getValue().split(","));
                rules.add(new Rule("dataset","string", "in", datasets));
                inputData.setDatasets(datasets);
            }
            else if(parameter.getName().equals("pathology"))
                inputData.setPathology(parameter.getValue());

            else if(parameter.getName().equals("filter")){
                if (parameter.getValue() != "")
                    rules.add(JsonConverters.convertJsonStringToObject(parameter.getValue(), MIPEngineExperimentDTO.Filter.class));
            }
            else{
                mipEngineParameters.put(parameter.getName(), Arrays.asList(parameter.getValue().split(",")));
            }
        });
        MIPEngineExperimentDTO.Filter filter = new MIPEngineExperimentDTO.Filter("AND", rules, true);
        inputData.setFilters(filter);
        mipEngineExperimentDTO.setInputdata(inputData);
        mipEngineExperimentDTO.setParameters(mipEngineParameters);
        return mipEngineExperimentDTO;
    }
}
