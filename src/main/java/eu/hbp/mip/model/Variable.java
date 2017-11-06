/**
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`variable`")
@ApiModel
@JsonIgnoreProperties(value = { "queries" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Variable {

    @Id
    @Expose
    private String code = null;

    /**
     *  Empty constructor is needed by Hibernate
     */
    public Variable() {
    }

    public Variable(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }



}
