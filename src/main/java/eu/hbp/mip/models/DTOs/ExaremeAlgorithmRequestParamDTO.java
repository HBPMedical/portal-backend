package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

//The request of an exareme algorithm is a list of ExaremeAlgorithmRequestParamDTOs.
@Getter
@Setter
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
}
