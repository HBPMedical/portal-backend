/**
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.hbp.mip.utils.CustomResourceLoader;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import eu.hbp.mip.utils.UserActionLogging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/pathologies")
public class PathologiesApi {

    @RequestMapping(name = "/pathologies", method = RequestMethod.GET)
    public String getPathologies() {
		UserActionLogging.LogAction("load the pathologies", "");
		
        return loadPathologies();
    }

    @Autowired
    private CustomResourceLoader resourceLoader;
    private String loadPathologies() {

        Resource resource  = resourceLoader.getResource("file:/opt/portal/api/pathologies.json");
        String result;
        try {
            result = convertInputStreamToString(resource.getInputStream());
        } catch (IOException e) {
            result = "{\"error\" : \"The pathologies.json file could not be read.\"}";
        }
        return result;
    }

    // Pure Java
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString(StandardCharsets.UTF_8.name());

    }
}
