/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@ApiModel
@Table(name = "`query`")
@JsonIgnoreProperties(value = {"id"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id = null;

    @ManyToMany
    @JoinTable(name = "query_variable", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> variables = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "query_covariable", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> covariables = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "query_grouping", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> grouping = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "query_training_datasets", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> trainingDatasets = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "query_testing_datasets", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> testingDatasets = new LinkedList<>();

    @ManyToMany
    @JoinTable(name = "query_validation_datasets", joinColumns = {
            @JoinColumn(name = "id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "code",
                    nullable = false, updatable = false)})
    private List<Variable> validationDatasets = new LinkedList<>();

    @Column(columnDefinition = "text")
    private String filters = "";

    @Column(columnDefinition = "text")
    private String pathology = "";


    public Query() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    @JsonProperty("variables")
    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }


    @JsonProperty("coVariables")
    public List<Variable> getCovariables() {
        return covariables;
    }

    public void setCovariables(List<Variable> covariables) {
        this.covariables = covariables;
    }


    @JsonProperty("groupings")
    public List<Variable> getGrouping() {
        return grouping;
    }

    public void setGrouping(List<Variable> grouping) {
        this.grouping = grouping;
    }

    @JsonProperty("trainingDatasets")
    public List<Variable> getTrainingDatasets() {
        return trainingDatasets;
    }

    public void setTrainingDatasets(List<Variable> trainingDatasets) {
        this.trainingDatasets = trainingDatasets;
    }

    @JsonProperty("testingDatasets")
    public List<Variable> getTestingDatasets() {
        return testingDatasets;
    }

    public void setTestingDatasets(List<Variable> testingDatasets) {
        this.testingDatasets = testingDatasets;
    }

    @JsonProperty("validationDatasets")
    public List<Variable> getValidationDatasets() {
        return validationDatasets;
    }

    public void setValidationDatasets(List<Variable> validationDatasets) {
        this.validationDatasets = validationDatasets;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public String getPathology() {
        return pathology;
    }

    public void setPathology(String pathology) {
        this.pathology = pathology;
    }

}
