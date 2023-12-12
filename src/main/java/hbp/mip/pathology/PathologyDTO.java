package hbp.mip.pathology;

import java.util.List;


public record PathologyDTO(
        String code,
        String version,
        String label,
        Boolean longitudinal,
        PathologyMetadataDTO metadataHierarchy,
        List<EnumerationDTO> datasets
) {
    public record EnumerationDTO(String code, String label) {
    }
}

