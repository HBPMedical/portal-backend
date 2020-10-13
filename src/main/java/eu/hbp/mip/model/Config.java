/**
 * Created by mirco on 25.02.16.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;

import javax.persistence.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "`config`")
@ApiModel
@JsonIgnoreProperties(value = {"id"})
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id = null;

    private String type = null;

    private Integer height = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "config_yAxisVariables", joinColumns = @JoinColumn(name = "config_id"))
    private List<String> yAxisVariables = new LinkedList<>();

    private String xAxisVariable = null;

    private Boolean hasXAxis = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "config_title", joinColumns = @JoinColumn(name = "config_id"))
    private Map<String, String> title = new HashMap<>();


    public Config() {
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


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }


    public List<String> getyAxisVariables() {
        return yAxisVariables;
    }

    public void setyAxisVariables(List<String> yAxisVariables) {
        this.yAxisVariables = yAxisVariables;
    }


    public String getxAxisVariable() {
        return xAxisVariable;
    }

    public void setxAxisVariable(String xAxisVariable) {
        this.xAxisVariable = xAxisVariable;
    }


    public Boolean getHasXAxis() {
        return hasXAxis;
    }

    public void setHasXAxis(Boolean hasXAxis) {
        this.hasXAxis = hasXAxis;
    }


    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

}
