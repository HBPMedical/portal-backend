/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`user`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDAO {

    @Id
    @Expose
    private String username;

    @Expose
    private String fullname;

    @Expose
    private String email;

    @Expose
    private Boolean agreeNDA;

    public UserDAO() {
        // Empty constructor is needed by Hibernate
    }

    public UserDAO(String username, String fullname, String email) {
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.agreeNDA = false;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAgreeNDA() {
        return agreeNDA;
    }

    public void setAgreeNDA(Boolean agreeNDA) {
        this.agreeNDA = agreeNDA;
    }
}
