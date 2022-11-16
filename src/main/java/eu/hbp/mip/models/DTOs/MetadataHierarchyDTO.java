package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class MetadataHierarchyDTO {

    @SerializedName("variables")
    private List<CommonDataElement> variables;

    @SerializedName("code")
    private String code;

    @SerializedName("groups")
    private List<MetadataHierarchyDTO> groups;

    @SerializedName("label")
    private String label;
    @Data
    @AllArgsConstructor
    public static class CommonDataElement {
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

        private void updateEnumerations(){
            if (this.enumerations != null){
                Map old_enumeration = (Map) this.enumerations;
                List<PathologyDTO.EnumerationDTO> enumerationDTOS = new ArrayList<>();
                old_enumeration.forEach((cdeCode, cdeLabel) -> {
                    enumerationDTOS.add(new PathologyDTO.EnumerationDTO((String) cdeCode, (String) cdeLabel));
                });
                setEnumerations(enumerationDTOS);
            }
        }
    }

    public void updateVariableWithProperEnums(){
        List<CommonDataElement> updated_variables = new ArrayList<>();
        this.variables.forEach(commonDataElement -> {
            commonDataElement.updateEnumerations();
            updated_variables.add(commonDataElement);
        });
        setVariables(updated_variables);
    }

    public void updateGroupWithProperEnums(){
        List<MetadataHierarchyDTO> updated_groups = new ArrayList<>();
        for (MetadataHierarchyDTO hierarchyDTO : this.groups) {

            if (hierarchyDTO.getVariables() != null) {
                hierarchyDTO.updateVariableWithProperEnums();
            }

            if (hierarchyDTO.getGroups() != null) {
                hierarchyDTO.updateGroupWithProperEnums();
            }
            updated_groups.add(hierarchyDTO);
        }
        this.groups = updated_groups;
    }

    public boolean isDatasetCDEPresent(){
        if (this.variables != null) {
            for (CommonDataElement variable : this.variables) {
                if (variable.code.equals("dataset")){
                    return true;
                }
            }

        }
        if (this.groups != null) {
            for (MetadataHierarchyDTO group: this.groups){
                if (group.isDatasetCDEPresent()){
                    return true;
                }
            }
        }
        return false;
    }

    public void updateDatasetCde(List<PathologyDTO.EnumerationDTO> pathologyDatasetDTOS){
        if (this.variables != null) {
            List<MetadataHierarchyDTO.CommonDataElement> variables = this.variables;
            variables.stream().filter(cde -> cde.getCode().equals("dataset")).
                    findAny().ifPresent(cde -> cde.setEnumerations(pathologyDatasetDTOS));
        }

        if (this.groups != null) {
            for (MetadataHierarchyDTO group: this.groups){
                group.updateDatasetCde(pathologyDatasetDTOS);
            }
        }
    }
}
