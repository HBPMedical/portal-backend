/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model.DAOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "`user`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDAO {

    @Id
    @Expose
    private String username = null;

    @Expose
    private String fullname = null;

    @Expose
    private String email = null;

    private Boolean agreeNDA = null;


    public UserDAO() {
        /*
         *  Empty constructor is needed by Hibernate
         */
    }


    /**
     * Create a user using OpenID user profile
     *
     * @param userInfo info from OpenID UserInfo endpoint
     */
    public UserDAO(String userInfo) {


        // TODO fix
        this.username = "test";
        this.fullname = "test";
        this.email = "test";

        //
//        Pattern p;
//        Matcher m;
//
//        p = Pattern.compile("preferred_username=([\\w- ]+)");
//        m = p.matcher(userInfo);
//        if (m.find()) {
//            this.username = m.group(1);
//        }
//
//        p = Pattern.compile("name=([\\w ]+)");
//        m = p.matcher(userInfo);
//        if (m.find()) {
//            this.fullname = m.group(1);
//        }
//
//        p = Pattern.compile("email=([\\w.]+@[\\w.]+)");
//        m = p.matcher(userInfo);
//        if (m.find()) {
//            this.email = m.group(1);
//        }


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
