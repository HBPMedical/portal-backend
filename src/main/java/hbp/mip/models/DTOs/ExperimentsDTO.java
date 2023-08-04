package hbp.mip.models.DTOs;

import java.util.List;

public record ExperimentsDTO(
        List<ExperimentDTO> experiments,
        Integer currentPage,
        Integer totalPages,
        Long totalExperiments
) {
}
