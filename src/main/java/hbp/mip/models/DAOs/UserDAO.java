package hbp.mip.models.DAOs;

import com.google.gson.annotations.Expose;
import hbp.mip.models.DTOs.UserDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "`user`")
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

    public UserDAO(String username, String fullname, String email, String subjectId) {
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.agreeNDA = false;
        this.subjectId = subjectId;
    }

    public UserDAO(UserDTO userDTO) {
        this.username = userDTO.username();
        this.fullname = userDTO.fullname();
        this.email = userDTO.email();
        this.agreeNDA = userDTO.agreeNDA();
        this.subjectId = userDTO.subjectId();
    }
}
