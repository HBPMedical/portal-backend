package hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import hbp.mip.utils.Exceptions.InternalServerError;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

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
    private List<EnumerationDTO> datasets;

    public PathologyDTO(){

    }


    public PathologyDTO(String pathology, Exareme2AttributesDTO exareme2AttributesDTO, List<EnumerationDTO> pathologyDatasetDTOS) {
        MetadataHierarchyDTO metadataHierarchyDTO = exareme2AttributesDTO.getProperties().get("cdes").get(0);
        if (!metadataHierarchyDTO.isDatasetCDEPresent()) throw new InternalServerError("CommonDataElement Dataset was not present in the pathology:" + pathology);
        metadataHierarchyDTO.updateDatasetCde(pathologyDatasetDTOS);

        List<String> pathology_info = Arrays.asList(pathology.split(":", 2));
        this.code = pathology_info.get(0);
        this.version = pathology_info.get(1);
        this.metadataHierarchyDTO = metadataHierarchyDTO;
        this.label = metadataHierarchyDTO.getLabel();
        this.datasets = pathologyDatasetDTOS;
    }

    @Data
    @AllArgsConstructor
    public static class EnumerationDTO {
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
