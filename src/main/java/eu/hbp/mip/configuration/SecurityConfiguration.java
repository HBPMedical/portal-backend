package eu.hbp.mip.configuration;

import eu.hbp.mip.utils.CORSFilter;
import eu.hbp.mip.utils.CustomAccessDeniedHandler;
import eu.hbp.mip.utils.CustomLoginUrlAuthenticationEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.net.ssl.*;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// Reference for OAuth2 login: https://spring.io/guides/tutorials/spring-boot-oauth2/
// also http://cscarioni.blogspot.ch/2013/04/pro-spring-security-and-oauth-2.html
// Security with Keycloak: https://www.thomasvitale.com/keycloak-authentication-flow-sso-client/

@Configuration
@EnableOAuth2Client
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Qualifier("oauth2ClientContext")
    @Autowired
    private OAuth2ClientContext oauth2ClientContext;

    /**
     * Enable HBP collab authentication (1) or disable it (0). Default is 1
     */
    @Value("#{'${hbp.authentication.enabled}'}")
    private boolean authenticationEnabled;

    /**
     * Absolute URL to redirect to when login is required
     */
    @Value("#{'${frontend.loginUrl}'}")
    private String loginUrl;

    /**
     * Absolute URL to redirect to when logout is required
     */
    @Value("#{'${hbp.client.logoutUri}'}")
    private String logoutUrl;

    /**
     * Absolute URL to redirect to after successful login
     */
    @Value("#{'${frontend.redirectAfterLoginUrl}'}")
    private String frontendRedirectAfterLogin;

    /**
     * Absolute URL to redirect to after successful logout
     */
    @Value("#{'${frontend.redirectAfterLogoutUrl}'}")
    private String redirectAfterLogoutUrl;

    public boolean getAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public String getFrontendRedirectAfterLogin() {
        return frontendRedirectAfterLogin;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        disableCertificateValidation();
        // @formatter:off
        http.addFilterBefore(new CORSFilter(), ChannelProcessingFilter.class);

        if (authenticationEnabled) {
            http.antMatcher("/**")
                    .authorizeRequests()
                    .antMatchers(
                            "/", "/login/**", "/health/**", "/info/**", "/metrics/**",
                            "/trace/**", "/frontend/**", "/webjars/**", "/v2/api-docs",
                            "/swagger-ui.html", "/swagger-resources/**"
                    ).permitAll()
                    .antMatchers("/galaxy*", "/galaxy/*").hasRole("Data Manager")
                    .anyRequest().hasRole("Researcher")
                    .and().exceptionHandling().authenticationEntryPoint(new CustomLoginUrlAuthenticationEntryPoint(loginUrl))
                    .accessDeniedHandler(new CustomAccessDeniedHandler())
                    .and().logout().addLogoutHandler(authLogoutHandler()).logoutSuccessUrl(redirectAfterLogoutUrl)
                    .and().logout().permitAll()
                    .and().csrf().ignoringAntMatchers("/logout").csrfTokenRepository(csrfTokenRepository())
                    .and().addFilterAfter(csrfHeaderFilter(), CsrfFilter.class)
                    .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        } else {
            http.antMatcher("/**")
                    .authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .and().csrf().disable();
        }
    }

    private Filter ssoFilter() {
        OAuth2ClientAuthenticationProcessingFilter hbpFilter = new OAuth2ClientAuthenticationProcessingFilter("/login/hbp");
        OAuth2RestTemplate hbpTemplate = new OAuth2RestTemplate(hbp(), oauth2ClientContext);
        hbpFilter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler(frontendRedirectAfterLogin));
        hbpFilter.setRestTemplate(hbpTemplate);
        hbpFilter.setTokenServices(new UserInfoTokenServices(hbpResource().getUserInfoUri(), hbp().getClientId()));
        return hbpFilter;
    }

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(
            OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    @Bean(name = "hbp")
    @ConfigurationProperties("hbp.client")
    public BaseOAuth2ProtectedResourceDetails hbp() {
        return new AuthorizationCodeResourceDetails();
    }

    @Bean(name = "hbpResource")
    @ConfigurationProperties("hbp.resource")
    public ResourceServerProperties hbpResource() {
        return new ResourceServerProperties();
    }


    private Filter csrfHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrf != null) {
                    Cookie cookie = WebUtils.getCookie(request, "XSRF-TOKEN");
                    String token = csrf.getToken();
                    if (cookie == null || token != null && !token.equals(cookie.getValue())) {
                        cookie = new Cookie("XSRF-TOKEN", token);
                        cookie.setPath("/");
                        response.addCookie(cookie);
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Bean
    public AuthoritiesExtractor keycloakAuthoritiesExtractor() {
        return new KeycloakAuthoritiesExtractor();
    }


    public class KeycloakAuthoritiesExtractor
            implements AuthoritiesExtractor {

        @Override
        public List<GrantedAuthority> extractAuthorities
                (Map<String, Object> map) {
            return AuthorityUtils
                    .commaSeparatedStringToAuthorityList(asAuthorities(map));
        }

        private String asAuthorities(Map<String, Object> map) {
            List<String> authorities = new ArrayList<>();
//            authorities.add("BAELDUNG_USER");
            List<LinkedHashMap<String, String>> authz;
            authz = (List<LinkedHashMap<String, String>>) map.get("authorities");
            for (LinkedHashMap<String, String> entry : authz) {
                authorities.add(entry.get("authority"));
            }
            return String.join(",", authorities);
        }
    }


    private LogoutHandler authLogoutHandler() {
        return (request, response, authentication) -> {
            logout();
        };
    }


    public void logout() {
        // TODO Try removing

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("client_id", hbp().getClientId());
        formParams.add("client_secret", hbp().getClientSecret());
        formParams.add("refresh_token", this.oauth2ClientContext.getAccessToken().getRefreshToken().getValue());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        RequestEntity<MultiValueMap<String, String>> requestEntity =
                new RequestEntity<>(formParams, httpHeaders, HttpMethod.POST,
                        URI.create(logoutUrl));
        restTemplate.exchange(requestEntity, String.class);
    }

    @Value("#{'${services.keycloak.keycloakUrl}'}")
    private String keycloakUrl;

    public void disableCertificateValidation() {

        //TODO Refactor logging

        LOGGER.info("disabling certificate validation host : " + keycloakUrl);

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};


        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv =
                (hostname, session) -> hostname.equals(keycloakUrl) && session.getPeerHost().equals(keycloakUrl);

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            // TODO add log message
        }

    }

}
