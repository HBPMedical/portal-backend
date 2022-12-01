package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

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
        private List<PathologyDTO.EnumerationDTO> enumerations;

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
