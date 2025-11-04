package hbp.mip.datamodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DataModelDTO(
        String code,
        String version,
        String label,
        Boolean longitudinal,
        List<CommonDataElementDTO> variables,
        List<DataModelGroupDTO> groups,
        List<DataModelDTO.EnumerationDTO> datasets,
        Map<String, List<String>> datasetsVariables
) {


    public DataModelDTO withDatasets(Map<String, List<String>> datasetVariablesByDataset) {
        // Find the datasets enumeration if it exists in variables or groups
        List<EnumerationDTO> datasets = findDatasetEnumerations(this.variables, this.groups);
        Map<String, List<String>> datasetsVariables = normaliseDatasetVariables(datasetVariablesByDataset);
        // Return a new instance of DataModelDTO with datasets and variables set appropriately
        return new DataModelDTO(this.code, this.version, this.label, this.longitudinal, this.variables, this.groups, datasets, datasetsVariables);
    }

    private static List<EnumerationDTO> findDatasetEnumerations(List<CommonDataElementDTO> variables, List<DataModelGroupDTO> groups) {
        // Check the top-level variables list for a dataset code
        Optional<List<EnumerationDTO>> datasetEnumerations = findDatasetInVariables(variables);
        if (datasetEnumerations.isPresent()) {
            return datasetEnumerations.get();
        }

        // If not found in the top level, search recursively in the groups
        for (DataModelGroupDTO group : groups) {
            datasetEnumerations = findDatasetInGroup(group);
            if (datasetEnumerations.isPresent()) {
                return datasetEnumerations.get();
            }
        }
        return null; // Return null if no dataset variable is found
    }

    private static Optional<List<EnumerationDTO>> findDatasetInVariables(List<CommonDataElementDTO> variables) {
        return variables.stream()
                .filter(variable -> "dataset".equals(variable.getCode()))
                .map(CommonDataElementDTO::getEnumerations)
                .findFirst();
    }

    private static Optional<List<EnumerationDTO>> findDatasetInGroup(DataModelGroupDTO group) {
        // First check the variables in the group
        Optional<List<EnumerationDTO>> datasetEnumerations = findDatasetInVariables(group.variables());
        if (datasetEnumerations.isPresent()) {
            return datasetEnumerations;
        }

        // Recursively search in subgroups
        for (DataModelGroupDTO subgroup : group.groups()) {
            datasetEnumerations = findDatasetInGroup(subgroup);
            if (datasetEnumerations.isPresent()) {
                return datasetEnumerations;
            }
        }
        return Optional.empty();
    }

    private static Map<String, List<String>> normaliseDatasetVariables(Map<String, List<String>> datasetVariablesByDataset) {
        if (datasetVariablesByDataset == null || datasetVariablesByDataset.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> normalisedVariables = new HashMap<>();
        datasetVariablesByDataset.forEach((datasetCode, variables) -> {
            if (datasetCode == null) {
                return;
            }
            List<String> safeVariables = (variables == null || variables.isEmpty())
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(variables));
            normalisedVariables.put(datasetCode, safeVariables);
        });

        if (normalisedVariables.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(normalisedVariables);
    }

    public record DataModelGroupDTO(
            String code,
            String label,
            List<CommonDataElementDTO> variables,
            List<DataModelGroupDTO> groups
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
        @Setter
        private List<DataModelDTO.EnumerationDTO> enumerations;
        private String min;
        private String max;
        private String type;
        private String methodology;
        private String units;
    }

    public record EnumerationDTO(String code, String label) {
    }
}
