package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MIPEngineAlgorithmResultDTO {
    private final String title;
    private final List<MIPEngineTabularVisualizationDTO.Field> columns;
    private final List<List<Object>> data;

    public MIPEngineTabularVisualizationDTO convertToVisualization() {
        HashMap<String, List<MIPEngineTabularVisualizationDTO.Field>> schema = new HashMap<>();
        schema.put("fields", columns);
        return new MIPEngineTabularVisualizationDTO(this.title, "tabular-data-resource", schema, this.data);
    }
}