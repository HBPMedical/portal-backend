package hbp.mip.algorithm;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AlgorithmSpecificationDTO(
        String name,
        String label,
        String desc,
        Exareme2AlgorithmInputdataSpecificationDTO inputdata,
        Map<String, AlgorithmParameterSpecificationDTO> parameters,
        List<TransformerSpecificationDTO> preprocessing,
        String type
) {
    @Override
    public Map<String, AlgorithmParameterSpecificationDTO> parameters() {
        return Objects.requireNonNullElse(parameters, Collections.EMPTY_MAP);
    }

    @Override
    public List<TransformerSpecificationDTO> preprocessing() {
        return Objects.requireNonNullElse(preprocessing, Collections.EMPTY_LIST);
    }

    public record AlgorithmParameterSpecificationDTO(
            String label,
            String desc,
            List<String> types,
            String notblank,
            String multiple,
            String min,
            String max,
            @SerializedName("default")
            String default_value,
            AlgorithmEnumDTO enums,
            AlgorithmEnumDTO dict_keys_enums,
            AlgorithmEnumDTO dict_values_enums

    ) {
        public record AlgorithmEnumDTO(
                String type,
                List<String> source
        ) {
        }
    }

    public record Exareme2AlgorithmInputdataSpecificationDTO(
            AlgorithmInputDataDetailSpecificationDTO x,
            AlgorithmInputDataDetailSpecificationDTO y,
            AlgorithmInputDataDetailSpecificationDTO data_model,
            AlgorithmInputDataDetailSpecificationDTO datasets,
            AlgorithmInputDataDetailSpecificationDTO filter
    ) {
    }

    public record AlgorithmInputDataDetailSpecificationDTO(
            String label,
            String desc,
            List<String> types,
            List<String> stattypes,
            String notblank,
            String multiple,
            Integer enumslen

    ) {
    }

    public record TransformerSpecificationDTO(
            String name,
            String label,
            String desc,
            Map<String, AlgorithmParameterSpecificationDTO> parameters
    ) {
        @Override
        public Map<String, AlgorithmParameterSpecificationDTO> parameters() {
            return Objects.requireNonNullElse(parameters, Collections.EMPTY_MAP);
        }
    }
}
