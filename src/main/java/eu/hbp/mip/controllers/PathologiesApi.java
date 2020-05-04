/**
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.model.PathologyDTO;
import eu.hbp.mip.model.UserInfo;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.CustomResourceLoader;
import eu.hbp.mip.utils.UserActionLogging;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/pathologies")
public class PathologiesApi {

    private static final Gson gson = new Gson();

    @Autowired
    private UserInfo userInfo;

    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${hbp.authentication.enabled:1}'}")
    private boolean authenticationIsEnabled;

    @Autowired
    private CustomResourceLoader resourceLoader;

    @RequestMapping(name = "/pathologies", method = RequestMethod.GET)
    public ResponseEntity<String> getPathologies(Authentication authentication) {
        UserActionLogging.LogUserAction(userInfo.getUser().getUsername(), "Load pathologies", "Running ...");

        // Load pathologies from file
        Resource resource = resourceLoader.getResource("file:/opt/portal/api/pathologies.json");
        List<PathologyDTO> allPathologies;
        try {
            allPathologies = gson.fromJson(convertInputStreamToString(resource.getInputStream()), new TypeToken<List<PathologyDTO>>() {
            }.getType());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("The pathologies.json file could not be read.");
        }

        // If authentication is disabled return everything
        if (!authenticationIsEnabled) {
            return ResponseEntity.ok().body(gson.toJson(allPathologies));
        }

        return ResponseEntity.ok().body(ClaimUtils.getAuthorizedPathologies(
                userInfo.getUser().getUsername(), authentication.getAuthorities(), allPathologies));
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
