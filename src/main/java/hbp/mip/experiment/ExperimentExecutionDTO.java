package hbp.mip.experiment;

import hbp.mip.algorithm.AlgorithmRequestDTO;

import java.util.Map;
import java.util.UUID;

public record ExperimentExecutionDTO(
                UUID uuid,
                String name,
                AlgorithmExecutionDTO algorithm,
                String mipVersion) {
        public record AlgorithmExecutionDTO(
                        String name,
                        AlgorithmRequestDTO.InputDataRequestDTO inputdata,
                        Map<String, Object> parameters,
                        Map<String, Object> preprocessing) {

        }
}
