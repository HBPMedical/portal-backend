package hbp.mip.models.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import hbp.mip.models.DAOs.UserDAO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    @SerializedName("username")
    private String username;

    @SerializedName("fullname")
    private String fullname;

    public UserDTO(){
    }

    public UserDTO(UserDAO userdao) {
        this.username = userdao.getUsername();
        this.fullname = userdao.getFullname();
    }
}

