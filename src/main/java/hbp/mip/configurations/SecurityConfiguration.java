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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final SpaRedirectAuthenticationSuccessHandler spaRedirectAuthenticationSuccessHandler;
    private final FrontendRedirectCaptureFilter frontendRedirectCaptureFilter;

    @Value("${authentication.enabled}")
    private boolean authenticationEnabled;

    @Value("${frontend.base-url:}")
    private String frontendBaseUrl;

    public SecurityConfiguration(SpaRedirectAuthenticationSuccessHandler spaRedirectAuthenticationSuccessHandler,
            FrontendRedirectCaptureFilter frontendRedirectCaptureFilter) {
        this.spaRedirectAuthenticationSuccessHandler = spaRedirectAuthenticationSuccessHandler;
        this.frontendRedirectCaptureFilter = frontendRedirectCaptureFilter;
    }

    // This Bean is used when there is no authentication and there is no keycloak
    // server running due to this bug:
    // https://github.com/spring-projects/spring-security/issues/11397#issuecomment-1655906163
    // So we overwrite the ClientRegistrationRepository Bean to avoid the IP server
    // lookup.
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
    SecurityFilterChain clientSecurityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepo) throws Exception {
        if (authenticationEnabled) {
            http.addFilterBefore(frontendRedirectCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class);
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/login/**",
                            "/oauth2/**",
                            "/actuator/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/**").authenticated());

            http.oauth2Login(login -> login.successHandler(spaRedirectAuthenticationSuccessHandler));

            // Allow API clients (e.g. notebooks) to authenticate with Bearer JWTs.
            // This runs alongside oauth2Login (session-based) authentication.
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

            // Open ID Logout
            // https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-logout
            OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(
                    clientRegistrationRepo);
            if (StringUtils.hasText(frontendBaseUrl)) {
                successHandler.setPostLogoutRedirectUri(frontendBaseUrl);
            }
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
                    // Bearer-token clients should not need CSRF (they are not cookie-authenticated).
                    .ignoringRequestMatchers((request) -> {
                        String authz = request.getHeader("Authorization");
                        return authz != null && authz.startsWith("Bearer ");
                    })
                    .ignoringRequestMatchers("/logout"));
            // <--- XSRF Token handling

        } else {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/**").permitAll());
            http.csrf((csrf) -> csrf
                    .ignoringRequestMatchers("/**"));

        }
        return http.build();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter fallback = new JwtGrantedAuthoritiesConverter();
        // Do not force ROLE_ prefix; the app expects raw authority strings (e.g. research_dataset_*).
        fallback.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            List<GrantedAuthority> out = new ArrayList<>();

            // 1) Preferred: our realm may map roles directly into an "authorities" claim.
            Object raw = jwt.getClaims().get("authorities");
            if (raw instanceof Collection<?> col) {
                for (Object v : col) {
                    if (v != null) {
                        out.add(new SimpleGrantedAuthority(v.toString()));
                    }
                }
            }

            // 2) Keycloak default: realm_access.roles
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> m) {
                Object roles = m.get("roles");
                if (roles instanceof Collection<?> col) {
                    for (Object v : col) {
                        if (v != null) {
                            out.add(new SimpleGrantedAuthority(v.toString()));
                        }
                    }
                }
            }

            // 3) Keycloak default: resource_access.<client>.roles (collect all client roles)
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (resourceAccess instanceof Map<?, ?> ra) {
                for (Object entryVal : ra.values()) {
                    if (!(entryVal instanceof Map<?, ?> m)) {
                        continue;
                    }
                    Object roles = m.get("roles");
                    if (!(roles instanceof Collection<?> col)) {
                        continue;
                    }
                    for (Object v : col) {
                        if (v != null) {
                            out.add(new SimpleGrantedAuthority(v.toString()));
                        }
                    }
                }
            }

            if (!out.isEmpty()) {
                return out;
            }

            // Fallback to scope-based authorities if present.
            return fallback.convert(jwt);
        });
        return converter;
    }

    @Component
    @RequiredArgsConstructor
    static class GrantedAuthoritiesMapperImpl implements GrantedAuthoritiesMapper {
        private static Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
            Collection<String> authorities = (Collection<String>) claims.get("authorities");
            if (authorities == null) {
                return Collections.emptyList(); // or throw a more informative exception if appropriate
            }
            return authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        @Override
        public Collection<? extends GrantedAuthority> mapAuthorities(
                Collection<? extends GrantedAuthority> authorities) {
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
