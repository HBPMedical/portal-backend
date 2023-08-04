package hbp.mip.models.DTOs.exareme2;


public record Exareme2PathologyCommonDataElementDTO(
        String code,
        String label,
        String description,
        String sql_type,
        Boolean is_categorical,
        Object enumerations,
        String min,
        String max,
        String units,
        String type,
        String methodology
) {
}
