package hbp.mip.configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SpaRedirectAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String REDIRECT_PATH_ATTRIBUTE = "SPA_REDIRECT_TARGET_PATH";
    public static final String FRONTEND_BASE_URL_ATTRIBUTE = "SPA_REDIRECT_FRONTEND_BASE_URL";

    private final String frontendBaseUrl;

    public SpaRedirectAuthenticationSuccessHandler(@Value("${frontend.base-url:}") String frontendBaseUrl) {
        // Don't crash the whole backend if this is missing. In production it SHOULD be set,
        // but dev tooling often runs without a full env-file loaded.
        this.frontendBaseUrl = StringUtils.hasText(frontendBaseUrl) ? normalizeBaseUrl(frontendBaseUrl) : null;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String targetPath = resolveTargetPath(request);
        if (!StringUtils.hasText(targetPath)) {
            // Fallback: do not fail the login. Just go to backend root.
            return request.getContextPath() + "/";
        }

        String baseUrl = resolveFrontendBaseUrl(request);
        if (!StringUtils.hasText(baseUrl)) {
            // No frontend base URL available. Fallback to relative redirect on the backend domain.
            return targetPath;
        }

        if ("/".equals(targetPath)) {
            return baseUrl + "/";
        }

        return baseUrl + targetPath;
    }

    private String resolveTargetPath(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attribute = session.getAttribute(REDIRECT_PATH_ATTRIBUTE);
            session.removeAttribute(REDIRECT_PATH_ATTRIBUTE);
            if (attribute instanceof String storedPath && !storedPath.isBlank()) {
                return normalizeRedirectPath(storedPath);
            }
        }
        return null;
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        if (StringUtils.hasText(frontendBaseUrl)) {
            return frontendBaseUrl;
        }

        // Dev fallback: FrontendRedirectCaptureFilter stores this based on Referer / frontend_redirect.
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attribute = session.getAttribute(FRONTEND_BASE_URL_ATTRIBUTE);
            session.removeAttribute(FRONTEND_BASE_URL_ATTRIBUTE);
            if (attribute instanceof String storedUrl && StringUtils.hasText(storedUrl)) {
                return normalizeBaseUrl(storedUrl);
            }
        }

        return null;
    }

    private static String normalizeRedirectPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String normalized = path.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
