/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`tag`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tag {

    @Id
    private String name = null;


    public Tag() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
