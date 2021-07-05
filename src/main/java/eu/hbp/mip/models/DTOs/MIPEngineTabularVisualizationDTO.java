package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MIPEngineTabularVisualizationDTO {
    private final String name;
    private final String profile;
    private final HashMap<String, List<Field>> schema;
    private final List<List<Object>> data;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Field {
        private final String name;
        private final String type;
    }
}

