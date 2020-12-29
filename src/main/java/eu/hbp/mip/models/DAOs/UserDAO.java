/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "`user`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDAO {

    @Id
    @Expose
    private String username;

    @Expose
    private String subjectId;

    @Expose
    private String fullname;

    @Expose
    private String email;

    @Expose
    private Boolean agreeNDA;

    public UserDAO() {
        // Empty constructor is needed by Hibernate
    }

    public UserDAO(String username, String fullname, String email, String subjectId) {
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.agreeNDA = false;
        this.subjectId = subjectId;

    }
}
