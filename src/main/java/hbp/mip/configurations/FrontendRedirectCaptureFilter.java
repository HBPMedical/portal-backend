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
            CapturedRedirect redirect = resolveRedirect(request);
            if (redirect != null && StringUtils.hasText(redirect.targetPath())) {
                HttpSession session = request.getSession(true);
                session.setAttribute(SpaRedirectAuthenticationSuccessHandler.REDIRECT_PATH_ATTRIBUTE, redirect.targetPath());
                if (StringUtils.hasText(redirect.frontendBaseUrl())) {
                    session.setAttribute(SpaRedirectAuthenticationSuccessHandler.FRONTEND_BASE_URL_ATTRIBUTE, redirect.frontendBaseUrl());
                }
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

    private CapturedRedirect resolveRedirect(HttpServletRequest request) {
        String explicitValue = request.getParameter("frontend_redirect");
        if (StringUtils.hasText(explicitValue)) {
            URI explicitUri = parseUri(explicitValue);
            if (explicitUri != null && StringUtils.hasText(explicitUri.getScheme()) && StringUtils.hasText(explicitUri.getRawAuthority())) {
                // frontend_redirect can be a full URL (e.g. http://localhost:4200/#/home).
                String baseUrl = explicitUri.getScheme() + "://" + explicitUri.getRawAuthority();
                String targetPath = buildPathQueryFragment(explicitUri);
                return new CapturedRedirect(baseUrl, targetPath);
            }

            // Otherwise treat it as a path (or hash route) and normalize.
            return new CapturedRedirect(null, normalizePath(explicitValue));
        }

        URI refererUri = parseUri(request.getHeader("Referer"));
        if (refererUri != null) {
            String baseUrl = null;
            if (StringUtils.hasText(refererUri.getScheme()) && StringUtils.hasText(refererUri.getRawAuthority())) {
                baseUrl = refererUri.getScheme() + "://" + refererUri.getRawAuthority();
            }
            String targetPath = buildPathQueryFragment(refererUri);
            return new CapturedRedirect(baseUrl, targetPath);
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

    private static String buildPathQueryFragment(URI uri) {
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String fragment = uri.getRawFragment();

        if (!StringUtils.hasText(path)) {
            path = "/";
        }

        String normalizedPath = normalizePath(path);
        if (StringUtils.hasText(query)) {
            normalizedPath += "?" + query;
        }
        if (StringUtils.hasText(fragment)) {
            normalizedPath += "#" + fragment;
        }

        return normalizedPath;
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

    private record CapturedRedirect(String frontendBaseUrl, String targetPath) {
    }
}
