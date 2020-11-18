package eu.hbp.mip.configurations;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;


@Controller
@KeycloakConfiguration
public class SecurityConfiguration extends KeycloakWebSecurityConfigurerAdapter {

    // Redirect to login page url
    private static final String logoutRedirectURL = "/sso/login";

    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationEnabled;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        if (authenticationEnabled) {
            http.authorizeRequests()
                    .antMatchers(
                            "/sso/login",
                            "/v2/api-docs", "/swagger-ui/**", "/swagger-resources/**"  // Swagger URLs
                    ).permitAll()
                    .antMatchers("/galaxy*", "/galaxy/*").hasRole("DATA MANAGER")
                    .anyRequest().hasRole("RESEARCHER");
        } else {
            http.antMatcher("/**")
                    .authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .and().csrf().disable();
        }
    }

    @Autowired
    private HttpServletRequest request;

    @GetMapping(value = "/logout")
    public String logout() throws ServletException {
        request.logout();
        return String.format("redirect:%s", logoutRedirectURL);
    }

    @Bean
    public KeycloakConfigResolver KeycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
        grantedAuthorityMapper.setConvertToUpperCase(true);

        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

}
