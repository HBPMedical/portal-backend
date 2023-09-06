package hbp.mip.configurations;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Value("${authentication.enabled}")
    private boolean authenticationEnabled;

    // This Bean is used when there is no authentication and there is no keycloak server running due to this bug:
    // https://github.com/spring-projects/spring-security/issues/11397#issuecomment-1655906163
    // So we overwrite the ClientRegistrationRepository Bean to avoid the IP server lookup.
    @Bean
    @ConditionalOnProperty(prefix = "authentication", name = "enabled", havingValue = "0")
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration dummyRegistration = ClientRegistration.withRegistrationId("dummy")
                .clientId("google-client-id")
                .clientSecret("google-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();
        return new InMemoryClientRegistrationRepository(dummyRegistration);
    }

    @Bean
    SecurityFilterChain clientSecurityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepo) throws Exception {
        if (authenticationEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/login/**",
                            "/oauth2/**",
                            "/actuator/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll()
                    .requestMatchers("/**").authenticated()
            );

            http.oauth2Login(login -> login.defaultSuccessUrl("/", true));

            // Open ID Logout
            // https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-logout
            OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepo);
            successHandler.setPostLogoutRedirectUri("{baseUrl}");
            http.logout(logout -> logout.logoutSuccessHandler(successHandler));

            // ---> XSRF Token handling
            // https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#deferred-csrf-token
            // https://stackoverflow.com/questions/74447118/csrf-protection-not-working-with-spring-security-6
            XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
            // set the name of the attribute the CsrfToken will be populated on
            requestHandler.setCsrfRequestAttributeName(null);

            // Change cookie path
            CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            tokenRepository.setCookiePath("/");

            http.csrf((csrf) -> csrf
                    .csrfTokenRepository(tokenRepository)
                    .csrfTokenRequestHandler(requestHandler::handle)
                    .ignoringRequestMatchers("/logout")
            );
            // <--- XSRF Token handling


        } else {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/**").permitAll()
            );
            http.csrf((csrf) -> csrf
                    .ignoringRequestMatchers("/**")
            );

        }
        return http.build();
    }

    @Component
    @RequiredArgsConstructor
    static class GrantedAuthoritiesMapperImpl implements GrantedAuthoritiesMapper {
        private static Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
            return ((Collection<String>) claims.get("authorities")).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        @Override
        public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    mappedAuthorities.addAll(extractAuthorities(oidcUserAuthority.getIdToken().getClaims()));
                }
            });

            return mappedAuthorities;
        }
    }
}

