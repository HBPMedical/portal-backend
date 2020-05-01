/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import io.swagger.annotations.ApiModel;

import javax.persistence.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Entity
@Table(name = "`user`")
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = { "appsVotes" })
public class User {

    @Id
    @Expose
    private String username = null;

    @Expose
    private String fullname = null;

    @Expose
    private String firstname = null;

    @Expose
    private String lastname = null;

    @Expose
    private String picture = null;

    @Expose
    private String web = null;

    @Expose
    private String phone = null;

    @Expose
    private String birthday = null;

    @Expose
    private String gender = null;

    @Expose
    private String city = null;

    @Expose
    private String country = null;

    @Expose
    private String password = null;

    @Expose
    private String email = null;

    @Expose
    private String apikey = null;

    @Expose
    private String team = null;

    @Expose
    private Boolean isActive = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_languages", joinColumns = @JoinColumn(name = "user_username"))
    @Expose
    private List<String> languages = new LinkedList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_username"))
    @Expose
    private List<String> roles = new LinkedList<>();

    private Boolean agreeNDA = null;


    public User() {
        /*
        *  Empty constructor is needed by Hibernate
        */
    }


    /**
     * Create a user using OpenID user profile
     * @param userInfo info from OpenID UserInfo endpoint
     */
    public User(String userInfo) {

        Pattern p;
        Matcher m;

        p = Pattern.compile("preferred_username=([\\w- ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.username = m.group(1);
        }

        p = Pattern.compile("name=([\\w ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.fullname = m.group(1);
        }

        p = Pattern.compile("given_name=([\\w ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.firstname = m.group(1);
        }

        p = Pattern.compile("family_name=([\\w ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.lastname = m.group(1);
        }

        p = Pattern.compile("email=([\\w.]+@[\\w.]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.email = m.group(1);
        }

        p = Pattern.compile("title=([\\w ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            if ("Mr".equals(m.group(1))) {
                this.gender = "Male";
            } else {
                this.gender = "Female";
            }
        }

        p = Pattern.compile("contractor=([\\w ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.team = m.group(1);
        }

        p = Pattern.compile("picture=([-a-zA-Z0-9+&@#/%=~_|.: ]+)");
        m = p.matcher(userInfo);
        if (m.find()) {
            this.picture = m.group(1);
        }

        if (this.picture == null || this.picture.isEmpty()) {
            this.picture = "images/users/default_user.png";
        }

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


    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }


    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }


    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }


    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }


    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }


    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }


    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }


    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }


    public Boolean getAgreeNDA() {
        return agreeNDA;
    }

    public void setAgreeNDA(Boolean agreeNDA) {
        this.agreeNDA = agreeNDA;
    }
}
