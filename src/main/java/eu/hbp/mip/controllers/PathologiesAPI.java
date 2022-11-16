package eu.hbp.mip.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.models.DTOs.MIPEngineAttributesDTO;
import eu.hbp.mip.models.DTOs.MetadataHierarchyDTO;
import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.*;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/pathologies", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/pathologies")
public class PathologiesAPI {

    private static final Gson gson = new Gson();


    // Enable HBP collab authentication (1) or disable it (0). Default is 1
    @Value("#{'${authentication.enabled}'}")
    private boolean authenticationIsEnabled;

    @Value("#{'${services.mipengine.attributesUrl}'}")
    private String mipengineAttributesUrl;

    @Value("#{'${services.mipengine.cdesMetadataUrl}'}")
    private String mipengineCDEsMetadataUrl;
    private final ActiveUserService activeUserService;

    public PathologiesAPI(ActiveUserService activeUserService) {
        this.activeUserService = activeUserService;
    }

    @RequestMapping(name = "/pathologies", method = RequestMethod.GET)
    public ResponseEntity<String> getPathologies(Authentication authentication) {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /pathologies");
        logger.LogUserAction("Loading pathologies ...");

        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = getMIPEngineDatasetsPerPathology(logger);

        Map<String, MIPEngineAttributesDTO> mipEnginePathologyAttributes = getMIPEnginePathologyAttributes(logger);

        List<PathologyDTO> pathologyDTOS = new ArrayList<>();
        for (String pathology : mipEnginePathologyAttributes.keySet()) {
            PathologyDTO newPathology;
            try {
                newPathology = new PathologyDTO(pathology, mipEnginePathologyAttributes.get(pathology), datasetsPerPathology.get(pathology));
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
        return ResponseEntity.ok().body(gson.toJson(ClaimUtils.getAuthorizedPathologies(logger, authentication, pathologyDTOS)));
    }

    public Map<String, List<PathologyDTO.EnumerationDTO>> getMIPEngineDatasetsPerPathology(Logger logger) {
        Map<String, Map<String, MetadataHierarchyDTO.CommonDataElement>> mipEngineCDEsMetadata;
        // Get MIPEngine algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(mipengineCDEsMetadataUrl, response);
            mipEngineCDEsMetadata = gson.fromJson(
                    response.toString(),
                    new TypeToken<HashMap<String, Map<String, MetadataHierarchyDTO.CommonDataElement>>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = new HashMap<>();

        mipEngineCDEsMetadata.forEach( (pathology, cdePerDataset) ->  {
            List<PathologyDTO.EnumerationDTO> pathologyDatasetDTOS = new ArrayList<>();
            Map datasetEnumerations = (Map) cdePerDataset.get("dataset").getEnumerations();
            datasetEnumerations.forEach((code, label) ->  pathologyDatasetDTOS.add(new PathologyDTO.EnumerationDTO((String) code, (String) label)));
            datasetsPerPathology.put(pathology, pathologyDatasetDTOS);
        });



        return datasetsPerPathology;
    }

    public Map<String, MIPEngineAttributesDTO> getMIPEnginePathologyAttributes(Logger logger) {
        Map<String, MIPEngineAttributesDTO> mipEnginePathologyAttributes;
        // Get MIPEngine algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(mipengineAttributesUrl, response);
            mipEnginePathologyAttributes = gson.fromJson(
                    response.toString(),
                    new TypeToken<HashMap<String, MIPEngineAttributesDTO>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        Map<String, MIPEngineAttributesDTO> mipEnginePathologyAttributesWithProperEnums = new HashMap<>();
        for (Map.Entry<String, MIPEngineAttributesDTO> entry : mipEnginePathologyAttributes.entrySet()) {
            String pathology = entry.getKey();
            MIPEngineAttributesDTO attributes = entry.getValue();
            attributes.updateAttributesWithProperEnums();
            mipEnginePathologyAttributesWithProperEnums.put(pathology, attributes);
        }

        return mipEnginePathologyAttributesWithProperEnums;
    }
}
