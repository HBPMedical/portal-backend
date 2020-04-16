package eu.hbp.mip.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(403);
        try {
            response.getWriter().write(new JSONObject()
                    .put("timestamp", new Timestamp(System.currentTimeMillis()))
                    .put("status", 403)
                    .put("error", "Forbidden")
                    .put("message", "Access Denied. Please contact the system administrator to request access.")
                    .put("path", request.getServletPath())
                    .toString());
        } catch (JSONException e) {
            response.getWriter().write("");
            e.printStackTrace();
        }
    }
}
