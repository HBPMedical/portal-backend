/**
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "`dataset`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dataset {

    @Id
    private String code = null;

    private Date date = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dataset_header", joinColumns = @JoinColumn(name = "dataset_code"))
    private List<String> header = new LinkedList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dataset_grouping", joinColumns = @JoinColumn(name = "dataset_code"))
    private List<String> grouping = new LinkedList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dataset_variable", joinColumns = @JoinColumn(name = "dataset_code"))
    private List<String> variable = new LinkedList<>();


    public Dataset() {
        /*
        *  Empty constructor is needed by Hibernate
        */
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public List<String> getHeader() {
        return header;
    }

    public void setHeader(List<String> header) {
        this.header = header;
    }


    public List<String> getGrouping() {
        return grouping;
    }

    public void setGrouping(List<String> grouping) {
        this.grouping = grouping;
    }


    public List<String> getVariable() {
        return variable;
    }

    public void setVariable(List<String> variable) {
        this.variable = variable;
    }

}
