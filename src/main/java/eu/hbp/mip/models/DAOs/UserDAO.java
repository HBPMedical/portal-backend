/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.models.DAOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@AllArgsConstructor
@Table(name = "`user`")
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
