package hbp.mip.models.DTOs;


import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Exareme2CommonDataElement {
    @SerializedName("is_categorical")
    private Boolean is_categorical;

    @SerializedName("code")
    private String code;

    @SerializedName("sql_type")
    private String sql_type;

    @SerializedName("description")
    private String description;

    @SerializedName("enumerations")
    private Object enumerations;

    @SerializedName("label")
    private String label;

    @SerializedName("units")
    private String units;

    @SerializedName("type")
    private String type;

    @SerializedName("methodology")
    private String methodology;

    @SerializedName("min")
    private String min;

    @SerializedName("max")
    private String max;
}
