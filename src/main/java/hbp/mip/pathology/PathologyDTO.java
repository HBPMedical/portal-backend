package hbp.mip.pathology;

import java.util.List;
import java.util.Map;


public record PathologyDTO(
        String code,
        String version,
        String label,
        Boolean longitudinal,
        PathologyMetadataDTO metadataHierarchy,
        List<EnumerationDTO> datasets,
        Map<String, List<String>> datasetsVariables
) {
    public record EnumerationDTO(String code, String label) {
    }
}
