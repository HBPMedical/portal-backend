package eu.hbp.mip.helpers;


import org.springframework.security.core.userdetails.UserDetails;

public class LogHelper {
    public static String logUser(UserDetails userDetails){
        return ("User(subject)->User(" + userDetails.getUsername() + ") : ");
    }
}