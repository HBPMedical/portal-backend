package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.models.DTOs.MetadataHierarchyDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class MIPEngineAttributesDTO {
    @SerializedName("properties")
    private Map<String, List<MetadataHierarchyDTO>> properties;

    @SerializedName("tags")
    private Object tags;

}
