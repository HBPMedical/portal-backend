package hbp.mip.pathology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public record PathologyMetadataDTO(
        String code,
        String version,
        String label,
        Boolean longitudinal,
        List<CommonDataElementDTO> variables,
        List<PathologyMetadataGroupDTO> groups
) {
    public record PathologyMetadataGroupDTO(
            String code,
            String label,
            List<CommonDataElementDTO> variables,
            List<PathologyMetadataGroupDTO> groups
    ) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommonDataElementDTO {
        private String code;
        private String label;
        private String description;
        private String sql_type;
        private String is_categorical;
        private List<PathologyDTO.EnumerationDTO> enumerations;
        private String min;
        private String max;
        private String type;
        private String methodology;
        private String units;

        public void setEnumerations(List<PathologyDTO.EnumerationDTO> enumerations) {
            this.enumerations = enumerations;
        }
    }
}