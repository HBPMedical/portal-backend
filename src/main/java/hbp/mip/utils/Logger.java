package hbp.mip.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public class Logger {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON Mapper

    /**
     * Constructor: Automatically sets MDC context with structured logging.
     */
    public Logger(String requestId, String username, Map<String, Object> requestDetails) {
        // Ensure request_id remains the same

        MDC.put("request_id", requestId);
        MDC.put("username", username);

        // Nest the request inside a "http" object
        if (requestDetails != null) {
            MDC.put("http", toJson(Map.of("request", requestDetails)));
        }
    }

    /**
     * Constructor: Generates a request ID automatically.
     */
    public Logger(String username, Map<String, Object> requestDetails) {
        this(UUID.randomUUID().toString(), username, requestDetails);
    }

    /**
     * Logs an action with structured data.
     */
    private void logUserAction(String message, String logLevel, Map<String, Object> additionalData) {
        try {
            // Extract response from additional data and update MDC
            if (additionalData != null && additionalData.get("response") instanceof Map) {
                Map<String, Object> response = (Map<String, Object>) additionalData.get("response");

                // Merge the existing "http" MDC field with the response
                String currentHttpContext = MDC.get("http");
                Map<String, Object> httpContext = currentHttpContext != null
                        ? objectMapper.readValue(currentHttpContext, Map.class)
                        : Map.of();

                // Add the response data
                httpContext = Map.of("request", httpContext.get("request"), "response", response);
                MDC.put("http", toJson(httpContext));
            }

            // Log with MDC context
            switch (logLevel) {
                case "ERROR" -> logger.error(message);
                case "WARN" -> logger.warn(message);
                case "DEBUG" -> logger.debug(message);
                default -> logger.info(message);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error structuring JSON log message", e);
        } finally {
            MDC.clear(); // Always clear MDC **AFTER** logging response
        }
    }

    public void error(String message) {
        logUserAction(message, "ERROR", null);
    }

    public void error(String message, Map<String, Object> additionalData) {
        logUserAction(message, "ERROR", additionalData);
        MDC.clear();
    }

    public void warn(String message) {
        logUserAction(message, "WARNING", null);
    }

    public void warn(String message, Map<String, Object> additionalData) {
        logUserAction(message, "WARNING", additionalData);
        MDC.clear();
    }

    public void info(String message) {
        logUserAction(message, "INFO", null);
    }

    public void info(String message, Map<String, Object> additionalData) {
        logUserAction(message, "INFO", additionalData);
        MDC.clear();
    }

    public void debug(String message) {
        logUserAction(message, "DEBUG", null);
    }

    public void debug(String message, Map<String, Object> additionalData) {
        logUserAction(message, "DEBUG", additionalData);
        MDC.clear();
    }

    /**
     * Converts a map to a JSON string for MDC storage.
     */
    private static String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}"; // Fallback in case of error
        }
    }
}
