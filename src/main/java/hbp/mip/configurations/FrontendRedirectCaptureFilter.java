package hbp.mip.configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
public class FrontendRedirectCaptureFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_PATH = "/oauth2/authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isAuthorizationRequest(request)) {
            String targetPath = resolveRedirectPath(request);
            if (targetPath != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute(SpaRedirectAuthenticationSuccessHandler.REDIRECT_PATH_ATTRIBUTE, targetPath);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorizationRequest(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String expectedPrefix = contextPath + AUTHORIZATION_PATH;
        return requestUri.startsWith(expectedPrefix);
    }

    private String resolveRedirectPath(HttpServletRequest request) {
        String explicitValue = request.getParameter("frontend_redirect");
        if (StringUtils.hasText(explicitValue)) {
            return normalizePath(explicitValue);
        }

        URI refererUri = parseUri(request.getHeader("Referer"));
        if (refererUri != null) {
            String path = refererUri.getRawPath();
            String query = refererUri.getRawQuery();

            if (!StringUtils.hasText(path)) {
                path = "/";
            }

            String normalizedPath = normalizePath(path);
            if (StringUtils.hasText(query)) {
                normalizedPath += "?" + query;
            }

            return normalizedPath;
        }

        return null;
    }

    private URI parseUri(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }

        String trimmed = path.trim();

        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }

        if (!trimmed.equals("/") && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }
}
