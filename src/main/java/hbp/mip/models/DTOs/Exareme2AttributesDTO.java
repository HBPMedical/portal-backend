package hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Exareme2AttributesDTO {
    @SerializedName("properties")
    private Map<String, List<MetadataHierarchyDTO>> properties;

    @SerializedName("tags")
    private Object tags;

}
