package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
@AllArgsConstructor
public class TabularVisualizationDTO {
    private final String name;
    private final String profile;
    private final HashMap<String, List<Field>> schema;
    private final List<List<Object>> data;


    public TabularVisualizationDTO(MIPEngineAlgorithmResultDTO mipEngineAlgorithmResultDTO) {
        HashMap<String, List<TabularVisualizationDTO.Field>> schema = new HashMap<>();
        List<Field> fields = new ArrayList<TabularVisualizationDTO.Field>();
        List<List<Object>> data = new ArrayList<ArrayList<TabularVisualizationDTO.Field>>()
        for (Column column : mipEngineAlgorithmResultDTO.getColumns())
        {
            fields.add(new Field(column));
            data.add(column.getData());
        }
        schema.put("fields", fields);
        this.name = mipEngineAlgorithmResultDTO.getTitle();
        this.profile = "tabular-data-resource";
        this.schema = schema;
        this.data = data;
    }
    @Data
    @AllArgsConstructor
    public static class Field {
        private final String name;
        private final String type;
        public Field(Column column)
        {
            this.name = column.getName();
            this.type = column.getType();
        }
    }
}

