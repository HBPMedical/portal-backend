/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`group`")
@ApiModel
@JsonIgnoreProperties(value = {"parent"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Group {

    @Id
    private String code = null;


    public Group() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }


    public String getCode() {
        return code;
    }

}
