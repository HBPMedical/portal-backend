package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

//The request of an exareme algorithm is a list of ExaremeAlgorithmRequestParamDTOs.
@Data
@AllArgsConstructor
public class ExaremeAlgorithmRequestParamDTO {
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
    public ExaremeAlgorithmRequestParamDTO (){}

    public ExaremeAlgorithmRequestParamDTO (String name, MIPEngineAlgorithmDTO.MIPEngineAlgorithmParameterDTO parameter){
        this.name = name;
        this.desc = parameter.getDesc();
        this.valueType = parameter.getType();
        this.type = "other";
        this.defaultValue = parameter.getDefault_value();
        this.valueNotBlank = parameter.getNotblank();
        this.label = parameter.getLabel();
        this.valueEnumerations = parameter.getEnums();
        this.valueMultiple = parameter.getMultiple();
        this.valueMax = parameter.getMin();
        this.valueMin = parameter.getMax();
    }

    public ExaremeAlgorithmRequestParamDTO (String name, MIPEngineAlgorithmDTO.MIPEngineAlgorithmInputDataDetailDTO inputDataDetail){
        this.name = name;
        this.desc = inputDataDetail.getDesc();
        this.value = "";
        this.valueNotBlank = inputDataDetail.getNotblank();
        this.valueMultiple = inputDataDetail.getMultiple();
        String[] hidden = {"x","y","dataset", "filter","pathology","centers","formula"};
        this.label = (Arrays.asList(hidden).contains(this.name) ? this.name : inputDataDetail.getLabel());
        if(name.equals("dataset") || name.equals("filter") || name.equals("pathology")){
            this.valueType = inputDataDetail.getTypes().get(0);
            this.type = this.name;
        }
        else{
            this.type = "column";
            this.columnValuesSQLType = String.join(", ", inputDataDetail.getTypes());
            this.columnValuesIsCategorical = getColumnValuesIsCategorical(inputDataDetail.getStattypes());
        }
    }

    private String getColumnValuesIsCategorical(List<String> stattypes){

        if (stattypes.contains("nominal") && stattypes.contains("numerical")){
            return "";
        }
        else if (stattypes.contains("nominal")){
            return "true";
        }
        else if (stattypes.contains("numerical")){
            return "false";
        }
        else{
            throw new InternalServerError("Invalid stattypes");
        }
    }
}
