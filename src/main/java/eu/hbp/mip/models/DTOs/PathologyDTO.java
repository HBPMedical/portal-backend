package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class PathologyDTO {

    @SerializedName("code")
    private String code;

    @SerializedName("version")
    private String version;


    @SerializedName("label")
    private String label;

    @SerializedName("metadataHierarchy")
    private MetadataHierarchyDTO metadataHierarchyDTO;

    @SerializedName("datasets")
    private List<PathologyDatasetDTO> datasets;

    public PathologyDTO(){

    }


    public PathologyDTO(String pathology, MIPEngineAttributesDTO mipEngineAttributesDTO, List<PathologyDatasetDTO> pathologyDatasetDTOS) {
        MetadataHierarchyDTO metadataHierarchyDTO = mipEngineAttributesDTO.getProperties().get("cdes").get(0);
        List<MetadataHierarchyDTO.CommonDataElement> variables = metadataHierarchyDTO.getVariables();
        variables.stream().filter(cde -> cde.getCode().equals("dataset")).
                findAny().ifPresent(cde -> cde.setEnumerations(pathologyDatasetDTOS));
        metadataHierarchyDTO.setVariables(variables);

        List<String> pathology_info = Arrays.asList(pathology.split(":", 2));
        this.code = pathology_info.get(0);
        this.version = pathology_info.get(1);
        this.metadataHierarchyDTO = metadataHierarchyDTO;
        this.label = metadataHierarchyDTO.getLabel();
        this.datasets = pathologyDatasetDTOS;
    }

    @Data
    @AllArgsConstructor
    public static class PathologyDatasetDTO {
        @SerializedName("code")
        private String code;

        @SerializedName("label")
        private String label;

        public String toString() {
            return code;
        }
    }

    public String toString() {
        return code;
    }
}
