package hbp.mip.models.DTOs;

import hbp.mip.models.DAOs.UserDAO;


public record UserDTO(String username, String fullname, String email, String subjectId, Boolean agreeNDA){
    public UserDTO(UserDAO userDAO){
        this(
                userDAO.getUsername(),
                userDAO.getFullname(),
                userDAO.getEmail(),
                userDAO.getSubjectId(),
                userDAO.getAgreeNDA()
        );
    }
}