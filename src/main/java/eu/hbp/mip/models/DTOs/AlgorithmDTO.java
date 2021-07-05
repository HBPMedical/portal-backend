package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class AlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private List<AlgorithmParamDTO> parameters;

    @Getter
    @Setter
    public static class AlgorithmParamDTO {
        @SerializedName("name")
        private String name;

        @SerializedName("desc")
        private String desc;

        @SerializedName("label")
        private String label;

        @SerializedName("type")
        private String type;

        @SerializedName("columnValuesSQLType")
        private String columnValuesSQLType;

        @SerializedName("columnValuesIsCategorical")
        private String columnValuesIsCategorical;

        @SerializedName("value")
        private String value;

        @SerializedName("defaultValue")
        private String defaultValue;

        @SerializedName("valueType")
        private String valueType;

        @SerializedName("valueNotBlank")
        private String valueNotBlank;

        @SerializedName("valueMultiple")
        private String valueMultiple;

        @SerializedName("valueMin")
        private String valueMin;

        @SerializedName("valueMax")
        private String valueMax;

        @SerializedName("valueEnumerations")
        private List<String> valueEnumerations;
    }

    @Getter
    @Setter
    public static class Rule
    {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        @SerializedName("operator")
        private String operator;

        @SerializedName("value")
        private Object value;

        public Rule(String id, String type, String operator, Object value) {
            this.id = id;
            this.type = type;
            this.operator = operator;
            this.value = value;
        }
    }

    @Getter
    @Setter
    public static class Filter
    {
        @SerializedName("condition")
        private String condition;

        @SerializedName("rules")
        private List<Object> rules;

        @SerializedName("valid")
        private boolean valid;

        public Filter(String condition, List<Object> rules, boolean valid) {
            this.condition = condition;
            this.rules = rules;
            this.valid = valid;
        }
    }


    public MIPEngineBody convertToMIPEngineBody()
    {
        MIPEngineBody mipEngineBody = new MIPEngineBody();
        MIPEngineBody.InputData inputData = new MIPEngineBody.InputData();
        Hashtable<String, Object> mipEngineParameters = new Hashtable<>();

        List<Object> rules = new ArrayList<>();
        this.parameters.forEach(parameter -> {

            if(parameter.getName().equals("x")) {
                System.out.println("x");
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
                    rules.add(JsonConverters.convertJsonStringToObject(parameter.getValue(), Filter.class));
            }
            else{
                mipEngineParameters.put(parameter.getName(), Arrays.asList(parameter.getValue().split(",")));
            }
        });
        Filter filter = new Filter("AND", rules, true);
        inputData.setFilters(filter);
        mipEngineBody.setInputdata(inputData);
        mipEngineBody.setParameters(mipEngineParameters);
        return mipEngineBody;
    }
}
