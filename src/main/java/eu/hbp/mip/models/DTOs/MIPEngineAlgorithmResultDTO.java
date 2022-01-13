package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MIPEngineAlgorithmResultDTO {
    private final String title;
    private final List<Column> columns;

    @Data
    @AllArgsConstructor
    public static class Column {
        private final String name;
        private final String type;
        private final List<Object> data;
    }
}