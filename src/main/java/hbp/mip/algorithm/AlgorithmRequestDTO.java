package hbp.mip.algorithm;

import hbp.mip.experiment.ExperimentExecutionDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public record AlgorithmRequestDTO(
        String request_id,
        InputDataRequestDTO inputdata,
        Map<String, Object> parameters,
        Map<String, Object> preprocessing
) {

    public static AlgorithmRequestDTO create(UUID experimentUUID,ExperimentExecutionDTO.AlgorithmExecutionDTO algorithmExecutionDTO){
        return new AlgorithmRequestDTO(
                experimentUUID.toString(),
                algorithmExecutionDTO.inputdata(),
                algorithmExecutionDTO.parameters(),
                algorithmExecutionDTO.preprocessing()
        );
    }

    public record InputDataRequestDTO(String data_model, List<String> datasets, List<String> x, List<String> y,
                                      FilterRequestDTO filters) {
    }

    public record FilterRequestDTO(String condition, List<Object> rules) {
    }
}
