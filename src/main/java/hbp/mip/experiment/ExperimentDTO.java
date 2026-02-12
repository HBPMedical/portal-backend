package hbp.mip.experiment;

import hbp.mip.user.UserDTO;
import hbp.mip.utils.JsonConverters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public record ExperimentDTO(
        UUID uuid,
        String name,
        UserDTO createdBy,
        Date created,
        Date updated,
        Date finished,
        Boolean shared,
        Boolean viewed,
        Object result,
        ExperimentDAO.Status status,
        ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm,
        String mipVersion) {
    public ExperimentDTO(ExperimentDAO experimentDAO, boolean includeResult) {
        this(
                experimentDAO.getUuid(),
                experimentDAO.getName(),
                new UserDTO(experimentDAO.getCreatedBy()),
                experimentDAO.getCreated(),
                experimentDAO.getUpdated(),
                experimentDAO.getFinished(),
                experimentDAO.isShared(),
                experimentDAO.isViewed(),
                includeResult
                        ? JsonConverters.convertJsonStringToObject(String.valueOf(experimentDAO.getResult()),
                                Object.class)
                        : null,
                experimentDAO.getStatus(),
                JsonConverters.convertJsonStringToObject(experimentDAO.getAlgorithm(),
                        ExperimentExecutionDTO.AlgorithmExecutionDTO.class),
                experimentDAO.getMipVersion());
    }
}
