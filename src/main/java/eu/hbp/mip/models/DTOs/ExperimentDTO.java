package eu.hbp.mip.models.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.hbp.mip.models.DAOs.ExperimentDAO;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
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
    private List<List<Object>> results;
    private ExperimentDAO.Status status;
    private AlgorithmDTO algorithm;

    public ExperimentDTO() {

    }
}
