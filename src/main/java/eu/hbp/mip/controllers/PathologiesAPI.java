package eu.hbp.mip.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.ClaimUtils;
import eu.hbp.mip.utils.CustomResourceLoader;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import eu.hbp.mip.utils.InputStreamConverter;
import eu.hbp.mip.utils.Logger;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/pathologies")
public class PathologiesAPI {

    private static final Gson gson = new Gson();


    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    @Value("#{'${files.pathologies_json}'}")
    private String pathologiesFilePath;

    private final ActiveUserService activeUserService;

    private final CustomResourceLoader resourceLoader;

    public PathologiesAPI(ActiveUserService activeUserService, CustomResourceLoader resourceLoader) {
        this.activeUserService = activeUserService;
        this.resourceLoader = resourceLoader;
    }

    @RequestMapping(name = "/pathologies", method = RequestMethod.GET)
    public ResponseEntity<String> getPathologies(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /pathologies");
        logger.LogUserAction("Loading pathologies ...");

        // Load pathologies from file
        Resource resource = resourceLoader.getResource(pathologiesFilePath);
        List<PathologyDTO> allPathologies;
        try {
            allPathologies = gson.fromJson(InputStreamConverter.convertInputStreamToString(resource.getInputStream()), new TypeToken<List<PathologyDTO>>() {
            }.getType());
        } catch (IOException e) {
            logger.LogUserAction("Unable to load pathologies");
            throw new BadRequestException("The pathologies could not be loaded.");
        }

        // If authentication is disabled return everything
        if (!authenticationIsEnabled) {
            logger.LogUserAction("Successfully loaded " + allPathologies.size() + " pathologies");
            return ResponseEntity.ok().body(gson.toJson(allPathologies));
        }

        logger.LogUserAction("Successfully loaded all authorized pathologies");
        return ResponseEntity.ok().body(ClaimUtils.getAuthorizedPathologies(logger,  authentication, allPathologies));
    }
}
