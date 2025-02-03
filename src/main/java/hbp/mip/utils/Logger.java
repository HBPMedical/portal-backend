package hbp.mip.utils;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Logger {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    /**
     * Constructor: Automatically sets MDC context with structured logging.
     */
    public Logger(String requestId, String username, Map<String, Object> requestDetails) {
        MDC.put("request_id", requestId);
        MDC.put("username", username);

        if (requestDetails != null) {
            // Flatten the request details before adding them to MDC
            Map<String, Object> flatRequestDetails = flattenMap(requestDetails, "http.request");
            flatRequestDetails.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
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
            if (additionalData != null) {
                // Flatten additional data and add it to MDC
                Map<String, Object> flatAdditionalData = flattenMap(additionalData, "http");
                flatAdditionalData.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
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
     * Recursively flattens a nested map into a single-level map with concatenated keys.
     */
    private static Map<String, Object> flattenMap(Map<String, Object> map, String parentKey) {
        Map<String, Object> flattenedMap = new HashMap<>();
        flattenMapHelper(map, parentKey.isEmpty() ? "" : parentKey + ".", flattenedMap);
        return flattenedMap;
    }

    private static void flattenMapHelper(Map<String, Object> map, String parentKey, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?>) {
                flattenMapHelper((Map<String, Object>) value, key + ".", result);
            } else {
                result.put(key, String.valueOf(value)); // Convert values to String for MDC
            }
        }
    }

}
