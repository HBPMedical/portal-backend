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
        // Result is a list of objects because there is a limitation that java has in types.
        // Exareme has result in the type of List<HashMap<String, Object>>
        // And there is no generic type that describes either an object or a list of objects
        List<Object> result,
        ExperimentDAO.Status status,
        ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm
) {
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
                includeResult ? JsonConverters.convertJsonStringToObject(String.valueOf(experimentDAO.getResult()), ArrayList.class) : null,
                experimentDAO.getStatus(),
                JsonConverters.convertJsonStringToObject(experimentDAO.getAlgorithm(), ExperimentExecutionDTO.AlgorithmExecutionDTO.class)
        );
    }
}
