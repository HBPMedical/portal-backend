package hbp.mip.configurations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class SpaRedirectAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String REDIRECT_PATH_ATTRIBUTE = "SPA_REDIRECT_TARGET_PATH";

    private final String frontendBaseUrl;

    public SpaRedirectAuthenticationSuccessHandler(@Value("${frontend.base-url}") String frontendBaseUrl) {
        Assert.hasText(frontendBaseUrl, "Property 'frontend.base-url' must be provided");
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String targetPath = resolveTargetPath(request);
        Assert.hasText(targetPath, "SPA redirect path was not captured. Ensure the frontend provides 'frontend_redirect'.");

        if ("/".equals(targetPath)) {
            return frontendBaseUrl + "/";
        }

        return frontendBaseUrl + targetPath;
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
