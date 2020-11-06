package eu.hbp.mip.model;

import eu.hbp.mip.model.DAOs.UserDAO;
import eu.hbp.mip.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.inject.Named;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Named("userInfo")
public class UserInfo {

    @Autowired
    private UserRepository userRepository;

    /**
     * Enable HBP collab authentication (1) or disable it (0). Default is 1
     */
    @Value("#{'${hbp.authentication.enabled}'}")
    private boolean authentication;

    private UserDAO user;

    /**
     * Set to true if using no-auth mode and user has clicked on the login button
     */
    private boolean fakeAuth = false;

    /**
     * returns the user for the current session.
     * <p>
     * the "synchronized" keyword is there to avoid a bug that the transaction is supposed to protect me from.
     * To test if your solution to removing it works, do the following:
     * - clean DB from scratch
     * - restart DB and backend (no session or anything like that)
     * - log in using the front end
     * - check you have no 500 error in the network logs.
     *
     * @return the user for the current session
     */
    public UserDAO getUser() {
        if (user == null) {

            if (!authentication) {
                user = new UserDAO();
                user.setUsername("anonymous");
                user.setFullname("anonymous");
                user.setEmail("anonymous@anonymous.com");
            } else {
                user = new UserDAO(getUserInformation());
            }
            UserDAO foundUser = userRepository.findByUsername(user.getUsername());
            if (foundUser != null) {
                user.setAgreeNDA(foundUser.getAgreeNDA());
            }
            userRepository.save(user);
        }

        return user;
    }

    public boolean getFakeAuth() {
        return fakeAuth;
    }

    public void setFakeAuth(boolean fakeAuth) {
        this.fakeAuth = fakeAuth;
    }

    private String getUserInformation() {
        // TODO
        return "";
        // get details from keycloak configuration
//        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
//        Authentication userAuthentication = oAuth2Authentication.getUserAuthentication();
//        return userAuthentication.getDetails().toString();
    }

}
