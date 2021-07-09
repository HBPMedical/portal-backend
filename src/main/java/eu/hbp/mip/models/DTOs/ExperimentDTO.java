package eu.hbp.mip.models.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.utils.JsonConverters;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperimentDTO {

    private UUID uuid;
    private String name;
    private String createdBy;
    private Date created;
    private Date updated;
    private Date finished;
    private Boolean shared;
    private Boolean viewed;
    // Result is a list of objects because there is a limitation that java has in types.
    // Exareme has result in the type of a List<HashMap<String, Object>>
    // Galaxy has result in the type of a List<HashMap<String, List<Object>>>
    //And there is no generic type that describes either an object or a list of objects
    private List<Object> result;
    private ExperimentDAO.Status status;
    private ExaremeAlgorithmDTO algorithm;

    public ExperimentDTO(){

    }
    public ExperimentDTO(boolean includeResult, ExperimentDAO experimentDAO)
    {
        this.algorithm = JsonConverters.convertJsonStringToObject(experimentDAO.getAlgorithm(), ExaremeAlgorithmDTO.class);
        this.created = experimentDAO.getCreated();
        this.updated = experimentDAO.getUpdated();
        this.finished = experimentDAO.getFinished();
        this.createdBy = experimentDAO.getCreatedBy().getUsername();
        this.name = experimentDAO.getName();
        if(includeResult){
            this.result = JsonConverters.convertJsonStringToObject(String.valueOf(experimentDAO.getResult()),  new ArrayList<>().getClass());
        }
        this.status = experimentDAO.getStatus();
        this.uuid = experimentDAO.getUuid();
        this.shared = experimentDAO.isShared();
        this.viewed = experimentDAO.isViewed();
    }
}
