package eu.hbp.mip.models.DTOs;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

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

    public MIPEngineBody convertToMIPEngineBody()
    {
        MIPEngineBody mipEngineBody = new MIPEngineBody();
        MIPEngineBody.InputData inputData = new MIPEngineBody.InputData();

        this.parameters.forEach(parameter -> {
            if(parameter.getName().equals("x"))
                inputData.setX(Arrays.asList(parameter.getValue().split(",")));
            if(parameter.getName().equals("y"))
                inputData.setY(Arrays.asList(parameter.getValue().split(",")));
            if(parameter.getName().equals("datasets"))
                inputData.setDatasets(Arrays.asList(parameter.getValue().split(",")));
            if(parameter.getName().equals("pathology"))
                inputData.setPathology(parameter.getValue());
        });
        mipEngineBody.setInputdata(inputData);
        return mipEngineBody;
    }
}
