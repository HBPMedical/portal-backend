package hbp.mip.models.DTOs.exareme2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Exareme2AlgorithmSpecificationDTO(
        String name,
        String label,
        String desc,
        String type,
        Exareme2AlgorithmInputdataSpecificationDTO inputdata,
        Map<String, Exareme2AlgorithmParameterSpecificationDTO> parameters,
        List<Exareme2TransformerSpecificationDTO> preprocessing
) {
    @Override
    public Map<String, Exareme2AlgorithmParameterSpecificationDTO> parameters() {
        return Objects.requireNonNullElse(parameters, Collections.EMPTY_MAP);
    }

    @Override
    public List<Exareme2TransformerSpecificationDTO> preprocessing() {
        return Objects.requireNonNullElse(preprocessing, Collections.EMPTY_LIST);
    }


    public record Exareme2AlgorithmParameterSpecificationDTO(
            String label,
            String desc,
            List<String> types,
            String notblank,
            String multiple,
            String min,
            String max,
            String default_value,
            Exareme2AlgorithmParameterSpecificationDTO.Exareme2AlgorithmEnumDTO enums,
            Exareme2AlgorithmEnumDTO dict_keys_enums,
            Exareme2AlgorithmEnumDTO dict_values_enums

    ) {
        public record Exareme2AlgorithmEnumDTO(
                String type,
                List<String> source
        ) {
        }
    }

    public record Exareme2AlgorithmInputdataSpecificationDTO(
            Exareme2AlgorithmInputDataDetailSpecificationDTO x,
            Exareme2AlgorithmInputDataDetailSpecificationDTO y,
            Exareme2AlgorithmInputDataDetailSpecificationDTO data_model,
            Exareme2AlgorithmInputDataDetailSpecificationDTO datasets,
            Exareme2AlgorithmInputDataDetailSpecificationDTO filter
    ) {
    }

    public record Exareme2AlgorithmInputDataDetailSpecificationDTO(
            String label,
            String desc,
            List<String> types,
            List<String> stattypes,
            String notblank,
            String multiple,
            Integer enumslen

    ) {
    }

    public record Exareme2TransformerSpecificationDTO(
            String name,
            String label,
            String desc,
            Map<String, Exareme2AlgorithmParameterSpecificationDTO> parameters
    ) {
        @Override
        public Map<String, Exareme2AlgorithmParameterSpecificationDTO> parameters() {
            return Objects.requireNonNullElse(parameters, Collections.EMPTY_MAP);
        }
    }
}
