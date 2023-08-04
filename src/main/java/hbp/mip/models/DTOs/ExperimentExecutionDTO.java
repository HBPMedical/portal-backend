package hbp.mip.models.DTOs;

import java.util.List;

public record ExperimentExecutionDTO(
        String name,
        AlgorithmExecutionDTO algorithm
) {
    public record AlgorithmExecutionDTO(
            String name,
            List<AlgorithmParameterExecutionDTO> parameters,
            List<TransformerExecutionDTO> preprocessing
    ) {
        public record TransformerExecutionDTO(String name, String value,
                                              List<AlgorithmParameterExecutionDTO> parameters) {
        }

        public record AlgorithmParameterExecutionDTO(String name, String value) {
        }
    }
}
