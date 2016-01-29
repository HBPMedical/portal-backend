/**
 * Created by mirco on 04.12.15.
 */

package org.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "`filter`")
@ApiModel(description = "")
@JsonIgnoreProperties(value = { "id" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Filter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;
    @ManyToOne
    private Variable variable = null;
    private String operator = null;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> values = new LinkedList<>();

    public Filter() {
    }

    /**
     * Unique identifier
     **/
    @ApiModelProperty(value = "Unique identifier")
    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Variable
     **/
    @ApiModelProperty(value = "Variable")
    @JsonProperty("variable")
    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    /**
     * Operator
     **/
    @ApiModelProperty(value = "Operator")
    @JsonProperty("operator")
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Filter {\n");

        sb.append("  id: ").append(id).append("\n");
        sb.append("  variable: ").append(variable).append("\n");
        sb.append("  operator: ").append(operator).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
