package eu.hbp.mip.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Component
public class PortalErrorAttributes extends DefaultErrorAttributes {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalErrorAttributes.class);

    @Override
    public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(requestAttributes, includeStackTrace);

        Throwable throwable = getError(requestAttributes);
        StringBuilder sb = new StringBuilder("[");
        for (String attr : requestAttributes.getAttributeNames(RequestAttributes.SCOPE_REQUEST)) {
            Object v = requestAttributes.getAttribute(attr, RequestAttributes.SCOPE_REQUEST);
            sb.append(attr).append(" = ").append(v).append('\n');
        }
        sb.append("]");
        LOGGER.error("Reporting server error on request with attributes " + sb.toString(), throwable);

        if (throwable != null) {

            Throwable cause = throwable.getCause();
            if (cause != null) {
                Map<String, Object> causeErrorAttributes = new HashMap<>();
                causeErrorAttributes.put("exception", cause.getClass().getName());
                causeErrorAttributes.put("message", cause.getMessage());
                errorAttributes.put("cause", causeErrorAttributes);
            }
        }

        return errorAttributes;
    }
}
