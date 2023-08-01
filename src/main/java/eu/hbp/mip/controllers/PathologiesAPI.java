package eu.hbp.mip.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.models.DTOs.Exareme2AttributesDTO;
import eu.hbp.mip.models.DTOs.Exareme2CommonDataElement;
import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.*;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
public class PathologiesAPI {

    private static final Gson gson = new Gson();

    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    @Value("#{'${services.exareme2.attributesUrl}'}")
    private String exareme2AttributesUrl;

    @Value("#{'${services.exareme2.cdesMetadataUrl}'}")
    private String exareme2CDEsMetadataUrl;

    private final ActiveUserService activeUserService;

    private final ClaimUtils claimUtils;

    public PathologiesAPI(ActiveUserService activeUserService, ClaimUtils claimUtils) {
        this.activeUserService = activeUserService;
        this.claimUtils = claimUtils;
    }

    @GetMapping
    public ResponseEntity<String> getPathologies(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser(authentication).getUsername(), "(GET) /pathologies");
        logger.LogUserAction("Loading pathologies ...");

        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = getExareme2DatasetsPerPathology(logger);

        Map<String, Exareme2AttributesDTO> exareme2PathologyAttributes = getExareme2PathologyAttributes(logger);

        List<PathologyDTO> pathologyDTOS = new ArrayList<>();
        for (String pathology : exareme2PathologyAttributes.keySet()) {
            PathologyDTO newPathology;
            try {
                newPathology = new PathologyDTO(pathology, exareme2PathologyAttributes.get(pathology), datasetsPerPathology.get(pathology));
            }
            catch(InternalServerError e) {
                logger.LogUserAction(e.getMessage());
                continue;
            }

            pathologyDTOS.add(newPathology);
        }

        // If authentication is disabled return everything
        if (!authenticationIsEnabled) {
            logger.LogUserAction("Successfully loaded " + pathologyDTOS.size() + " pathologies");
            return ResponseEntity.ok().body(gson.toJson(pathologyDTOS));
        }

        logger.LogUserAction("Successfully loaded all authorized pathologies");
        return ResponseEntity.ok().body(gson.toJson(claimUtils.getAuthorizedPathologies(logger, authentication, pathologyDTOS)));
    }

    public Map<String, List<PathologyDTO.EnumerationDTO>> getExareme2DatasetsPerPathology(Logger logger) {
        Map<String, Map<String, Exareme2CommonDataElement>> exareme2CDEsMetadata;
        // Get Exareme2 algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2CDEsMetadataUrl, response);
            exareme2CDEsMetadata = gson.fromJson(
                    response.toString(),
                    new TypeToken<HashMap<String, Map<String, Exareme2CommonDataElement>>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = new HashMap<>();

        exareme2CDEsMetadata.forEach( (pathology, cdePerDataset) ->  {
            List<PathologyDTO.EnumerationDTO> pathologyDatasetDTOS = new ArrayList<>();
            Map<String, String> datasetEnumerations = (Map<String, String>) cdePerDataset.get("dataset").getEnumerations();
            datasetEnumerations.forEach((code, label) ->  pathologyDatasetDTOS.add(new PathologyDTO.EnumerationDTO(code, label)));
            datasetsPerPathology.put(pathology, pathologyDatasetDTOS);
        });

        return datasetsPerPathology;
    }

    public Map<String, Exareme2AttributesDTO> getExareme2PathologyAttributes(Logger logger) {
        Map<String, Exareme2AttributesDTO> exareme2PathologyAttributes;
        // Get Exareme2 algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2AttributesUrl, response);
            exareme2PathologyAttributes = gson.fromJson(
                    response.toString(),
                    new TypeToken<HashMap<String, Exareme2AttributesDTO>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }


        return exareme2PathologyAttributes;
    }
}
