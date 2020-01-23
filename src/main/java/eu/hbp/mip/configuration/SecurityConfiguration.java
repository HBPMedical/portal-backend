package eu.hbp.mip.configuration;

import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.utils.CORSFilter;
import eu.hbp.mip.utils.CustomLoginUrlAuthenticationEntryPoint;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.UserActionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
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
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//newlyadded for logout
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.net.URI;


import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


// See https://spring.io/guides/tutorials/spring-boot-oauth2/ for reference about configuring OAuth2 login
// also http://cscarioni.blogspot.ch/2013/04/pro-spring-security-and-oauth-2.html

@Configuration
@EnableOAuth2Client
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

   private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

   @Autowired
   private OAuth2ClientContext oauth2ClientContext;

   /**
    * Enable HBP collab authentication (1) or disable it (0). Default is 1
    */
   @Value("#{'${hbp.authentication.enabled:1}'}")
   private boolean authentication;

   /**
    * Absolute URL to redirect to when login is required
    */
   @Value("#{'${frontend.loginUrl:/login/hbp}'}")
   private String loginUrl;
 
	/**
    * Absolute URL to redirect to when logout is required
    */
   @Value("#{'${hbp.client.logoutUri}'}")
   private String logoutUri;

   /**
    * Absolute URL to redirect to after successful login
    */
   @Value("#{'${frontend.redirectAfterLoginUrl:http://frontend/home}'}")
   private String frontendRedirectAfterLogin;

   /**
    * Absolute URL to redirect to after logout has occurred
    */
   @Value("#{'${frontend.redirectAfterLogoutUrl:/login/hbp}'}")
   private String redirectAfterLogoutUrl;

   /**
    * URL to revoke auth token
    */
   @Value("#{'${hbp.resource.revokeTokenUri:https://services.humanbrainproject.eu/oidc/revoke}'}")
   private String revokeTokenURI;
   
   

//    @Autowired
//    private HttpServletRequest request;

   @Override
   protected void configure(HttpSecurity http) throws Exception {
	   disableCertificateValidation();
       // @formatter:off
       http.addFilterBefore(new CORSFilter(), ChannelProcessingFilter.class);

       if (authentication) {
           http.antMatcher("/**")
                   .authorizeRequests()
                   .antMatchers(
                           "/", "/login/**", "/health/**", "/info/**", "/metrics/**", "/trace/**", "/frontend/**", "/webjars/**", "/v2/api-docs", "/swagger-ui.html", "/swagger-resources/**"
                   )
				   .permitAll()
                   .antMatchers("/galaxy*","/galaxy/*").hasRole("Data Manager")
				   //.anyRequest().authenticated()
				   .anyRequest().hasRole("Researcher")
                   .and().exceptionHandling().authenticationEntryPoint(new CustomLoginUrlAuthenticationEntryPoint(loginUrl))
                   .and().logout().addLogoutHandler(authLogoutHandler()).logoutSuccessUrl(redirectAfterLogoutUrl)
                   .and().logout().permitAll()
                   .and().csrf().ignoringAntMatchers("/logout").csrfTokenRepository(csrfTokenRepository())
				   .and().addFilterAfter(csrfHeaderFilter(), CsrfFilter.class)
                   .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
       }
       else {
            http.antMatcher("/**")
                    .authorizeRequests()
                    .antMatchers("/**").permitAll().and().csrf().disable();
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

   @Bean(name="hbp")
   @ConfigurationProperties("hbp.client")
   public BaseOAuth2ProtectedResourceDetails hbp() {
       return new AuthorizationCodeResourceDetails();
   }

   @Bean(name="hbpResource")
   @ConfigurationProperties("hbp.resource")
   public ResourceServerProperties hbpResource() {
       return new ResourceServerProperties();
   }

   public boolean isAuthentication() {
       return authentication;
   }

   public String getFrontendRedirectAfterLogin() {
       return frontendRedirectAfterLogin;
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

   private class CustomLogoutHandler implements LogoutHandler {
       @Override
       public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {

           // Hackish way of accessing to this information...
           final UserInfo userInfo = (UserInfo) httpServletRequest.getSession().getAttribute("userInfo");
           if (userInfo != null) {
               userInfo.setFakeAuth(false);
           }

           if (oauth2ClientContext == null || oauth2ClientContext.getAccessToken() == null)
           {
               return;
           }

           String idToken = oauth2ClientContext.getAccessToken().getAdditionalInformation().get("id_token").toString();

           StringBuilder query = new StringBuilder();
           query.append("{");
           query.append("\"token\":");
           query.append("\"").append(idToken).append("\"");
           query.append("}");

           try {
               int responseCode = HTTPUtil.sendPost(revokeTokenURI, query.toString(), new StringBuilder());
               if (responseCode != 200)
               {
                   LOGGER.warn("Cannot send request to OIDC server for revocation ! ");
               }
               else{
                   LOGGER.info("Should be logged out");
               }
           } catch (IOException e) {
               LOGGER.warn("Cannot notify logout to OIDC server !");
               LOGGER.trace("Cannot notify logout", e);
           }

       }
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
		// POSTするリクエストパラメーターを作成
		UserActionLogging.LogAction("refresh token ", this.oauth2ClientContext.getAccessToken().getRefreshToken().getValue());
		RestTemplate restTemplate = new RestTemplate();
		MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
		formParams.add("client_id", hbp().getClientId());
        formParams.add("client_secret", hbp().getClientSecret());
		formParams.add("refresh_token", this.oauth2ClientContext.getAccessToken().getRefreshToken().getValue());
		// リクエストヘッダーを作成
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		// リクエストを作成
		UserActionLogging.LogAction("logoutUri is ", logoutUri);
		RequestEntity<MultiValueMap<String, String>> requestEntity =
				new RequestEntity<>(formParams, httpHeaders, HttpMethod.POST,
						URI.create(logoutUri)); 
		// POSTリクエスト送信（ログアウト実行）

		ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
    }
   
   @Value("#{'${services.keycloak.keycloakUrl}'}")
   private String keycloakUrl;
   
    // static {
        // disableCertificateValidation();
    // }

    public void disableCertificateValidation() {
		LOGGER.info("disabling certificate validation host : " + keycloakUrl);
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                } };


        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
				
                // System.out.println("Warning: URL Host: " + hostname + " vs. "
                        // + session.getPeerHost());
                if(hostname.equals(keycloakUrl) && session.getPeerHost().equals(keycloakUrl))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {}

    } 

}
