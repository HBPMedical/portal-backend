package eu.hbp.mip.models.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperimentDTO {

    private UUID uuid;
    private String name;
    private UserDTO createdBy;
    private Date created;
    private Date updated;
    private Date finished;
    private Boolean shared;
    private Boolean viewed;
    // Result is a list of objects because there is a limitation that java has in types.
    // Exareme has result in the type of List<HashMap<String, Object>>
    //And there is no generic type that describes either an object or a list of objects
    private List<Object> result;
    private ExperimentDAO.Status status;
    private ExaremeAlgorithmDTO algorithm;


    public ExperimentDTO(boolean includeResult, ExperimentDAO experimentDAO) {
        this.algorithm = JsonConverters.convertJsonStringToObject(experimentDAO.getAlgorithm(), ExaremeAlgorithmDTO.class);
        this.created = experimentDAO.getCreated();
        this.updated = experimentDAO.getUpdated();
        this.finished = experimentDAO.getFinished();
        this.createdBy = new UserDTO(experimentDAO.getCreatedBy());
        this.name = experimentDAO.getName();
        if (includeResult) {
            this.result = JsonConverters.convertJsonStringToObject(String.valueOf(experimentDAO.getResult()), ArrayList.class);
        }
        this.status = experimentDAO.getStatus();
        this.uuid = experimentDAO.getUuid();
        this.shared = experimentDAO.isShared();
        this.viewed = experimentDAO.isViewed();
    }
}
