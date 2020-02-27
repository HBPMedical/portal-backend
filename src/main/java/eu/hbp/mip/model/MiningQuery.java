package eu.hbp.mip.model;

import com.google.gson.Gson;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mirco on 06.01.17.
 */
public class MiningQuery {

    private List<Variable> variables;
    private List<Variable> covariables;
    private List<Variable> grouping;
    private List<Variable> datasets;
    private String filters;
    private ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm;

    public MiningQuery() {
        this.variables = new LinkedList<>();
        this.covariables = new LinkedList<>();
        this.grouping = new LinkedList<>();
        this.datasets = new LinkedList<>();
        this.filters = "";
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public void addVariable(Variable variable) {
        this.variables.add(variable);
    }

    public List<Variable> getCovariables() {
        return covariables;
    }

    public void setCovariables(List<Variable> covariables) {
        this.covariables = covariables;
    }

    public void addCovariable(Variable variable) {
        this.covariables.add(variable);
    }

    public List<Variable> getGrouping() {
        return grouping;
    }

    public void setGrouping(List<Variable> grouping) {
        this.grouping = grouping;
    }

    public List<Variable> getDatasets() {
        return datasets;
    }

    public void setDataset(List<Variable> datasets) {
        this.datasets = datasets;
    }

    public void addDataset(Variable variable) {
        this.datasets.add(variable);
    }

    public void addGrouping(Variable variable) {
        this.grouping.add(variable);
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public ExperimentExecutionDTO.AlgorithmExecutionDTO getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(ExperimentExecutionDTO.AlgorithmExecutionDTO algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
