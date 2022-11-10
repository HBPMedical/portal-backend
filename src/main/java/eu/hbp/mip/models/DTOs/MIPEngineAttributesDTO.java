package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class MIPEngineAttributesDTO {
    @SerializedName("properties")
    private Map<String, List<MetadataHierarchyDTO>> properties;

    @SerializedName("tags")
    private Object tags;

    public void updateAttributesWithProperEnums(){
        Map<String, List<MetadataHierarchyDTO>> updated_properties = new HashMap<>();
        for (Map.Entry<String, List<MetadataHierarchyDTO>> entry : this.properties.entrySet()) {
            String pathology = entry.getKey();
            List<MetadataHierarchyDTO> hierarchyDTOS = entry.getValue();
            List<MetadataHierarchyDTO> updatedHierarchyDTOS = new ArrayList<>();

            for (MetadataHierarchyDTO hierarchyDTO : hierarchyDTOS) {
                hierarchyDTO.updateGroupWithProperEnums();
                updatedHierarchyDTOS.add(hierarchyDTO);
            }
            updated_properties.put(pathology,updatedHierarchyDTOS);
        }
        this.properties = updated_properties;
    }
}
