package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MIPEngineAlgorithmResultDTO {
    private final String title;
    private final List<TabularVisualizationDTO.Field> columns;
    private final List<List<Object>> data;
}