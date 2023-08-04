package hbp.mip.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hbp.mip.models.DTOs.exareme2.Exareme2PathologyCommonDataElementDTO;
import hbp.mip.models.DTOs.PathologyMetadataDTO;
import hbp.mip.models.DTOs.PathologyDTO;
import hbp.mip.utils.ClaimUtils;
import hbp.mip.utils.Exceptions.InternalServerError;
import hbp.mip.utils.HTTPUtil;
import hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PathologyService {

    private static final Gson gson = new Gson();

    private final ClaimUtils claimUtils;

    @Value("${authentication.enabled}")
    private boolean authenticationIsEnabled;

    @Value("${services.exareme2.attributesUrl}")
    private String exareme2AttributesUrl;

    @Value("${services.exareme2.cdesMetadataUrl}")
    private String exareme2CDEsMetadataUrl;

    public PathologyService(ClaimUtils claimUtils) {
        this.claimUtils = claimUtils;
    }

    public List<PathologyDTO> getPathologies(Authentication authentication, Logger logger) {
        List<PathologyDTO> allPathologyDTOs = getAggregatedPathologyDTOs(logger);

        if (!authenticationIsEnabled) {
            return allPathologyDTOs;
        }
        return claimUtils.getAuthorizedPathologies(logger, authentication, allPathologyDTOs);
    }

    /**
     * This method will fetch all necessary information about pathologies from exareme and then aggregate them.
     * The information that needs to be aggregated is the dataset CDE enumerations.
     * Exareme returns the information about the dataset enumerations in a separate endpoint.
     */
    private List<PathologyDTO> getAggregatedPathologyDTOs(Logger logger) {
        Map<String, PathologyMetadataDTO> pathologiesMetadataHierarchy = getExaremePathologiesMetadataHierarchyDTO(logger);
        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = getExareme2DatasetsPerPathology(logger);

        List<PathologyDTO> allPathologyDTOs = new ArrayList<>();
        for (String pathology : datasetsPerPathology.keySet()) {
            PathologyMetadataDTO pathologyMetadata = pathologiesMetadataHierarchy.get(pathology);
            assert pathologyMetadata != null;
            List<PathologyDTO.EnumerationDTO> pathologyDatasets = datasetsPerPathology.get(pathology);

            // Exareme collects the dataset CDE enumerations automatically from the nodes when there is an addition/deletion.
            // Exareme provides that information in a separate endpoint from the rest of the pathologies' metadata.
            // We need to manually update the dataset CDE enumerations in each pathology's metadata in order to
            // return the latest information in the frontend, without the need for data aggregation.
            if (!hasDatasetCDE(pathologyMetadata.variables(), pathologyMetadata.groups()))
                throw new InternalServerError("CommonDataElement 'dataset' was not present in the pathology's metadata:" + pathologyMetadata);
            updateDatasetCDEEnumerations(pathologyMetadata.variables(), pathologyMetadata.groups(), pathologyDatasets);

            allPathologyDTOs.add(
                    new PathologyDTO(
                            pathologyMetadata.code(),
                            pathologyMetadata.version(),
                            pathologyMetadata.label(),
                            pathologyMetadata.longitudinal(),
                            pathologyMetadata,
                            pathologyDatasets
                    )
            );
        }
        return allPathologyDTOs;
    }

    private Map<String, List<PathologyDTO.EnumerationDTO>> getExareme2DatasetsPerPathology(Logger logger) {
        Map<String, Map<String, Exareme2PathologyCommonDataElementDTO>> exareme2CDEsMetadata;
        Type exaremeCDEsMetadataType = new TypeToken<HashMap<String, Map<String, Exareme2PathologyCommonDataElementDTO>>>(){}.getType();
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2CDEsMetadataUrl, response);
            exareme2CDEsMetadata = gson.fromJson(response.toString(),exaremeCDEsMetadataType);
        } catch (IOException e) {
            logger.error("Could not fetch exareme2 datasets: " + e.getMessage());
            return new HashMap<>();
        }

        // Get the datasets for each pathology
        Map<String, List<PathologyDTO.EnumerationDTO>> datasetsPerPathology = new HashMap<>();
        exareme2CDEsMetadata.forEach((pathology, cdePerDataset) -> {
            List<PathologyDTO.EnumerationDTO> pathologyDatasetDTOS = new ArrayList<>();
            Map<String, String> datasetEnumerations = (Map<String, String>) cdePerDataset.get("dataset").enumerations();
            datasetEnumerations.forEach((code, label) -> pathologyDatasetDTOS.add(new PathologyDTO.EnumerationDTO(code, label)));
            datasetsPerPathology.put(pathology, pathologyDatasetDTOS);
        });

        return datasetsPerPathology;
    }

    private Map<String, Map<String, Map<String, List<Object>>>> getExareme2PathologyAttributes(Logger logger) {
        Map<String, Map<String, Map<String, List<Object>>>> exareme2PathologyAttributes;
        Type pathologyAttributesType = new TypeToken<Map<String, Map<String, Map<String, List<PathologyMetadataDTO>>>>>(){}.getType();
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2AttributesUrl, response);
            exareme2PathologyAttributes = gson.fromJson(response.toString(),pathologyAttributesType);
        } catch (IOException e) {
            logger.error("Could not fetch exareme2 pathologies' metadata: " + e.getMessage());
            return new HashMap<>();
        }

        return exareme2PathologyAttributes;
    }

    private Map<String, PathologyMetadataDTO> getExaremePathologiesMetadataHierarchyDTO(Logger logger) {
        Map<String, Map<String, Map<String, List<Object>>>> pathologiesAttributes = getExareme2PathologyAttributes(logger);

        Map<String, PathologyMetadataDTO> pathologiesHierarchies = new HashMap<>();
        pathologiesAttributes.forEach((pathology, attributes) -> {
            assert attributes.get("properties") != null;
            assert attributes.get("properties").get("cdes") != null;
            assert !attributes.get("properties").get("cdes").isEmpty();
            pathologiesHierarchies.put(pathology, (PathologyMetadataDTO) attributes.get("properties").get("cdes").get(0));
        });

        return pathologiesHierarchies;
    }


    private static boolean hasDatasetCDE(
            List<PathologyMetadataDTO.CommonDataElementDTO> variables,
            List<PathologyMetadataDTO.PathologyMetadataGroupDTO> groups
    ) {
        if (variables != null) {
            for (PathologyMetadataDTO.CommonDataElementDTO variable : variables) {
                if (variable.getCode().equals("dataset")){
                    return true;
                }
            }

        }
        if (groups != null) {
            for (PathologyMetadataDTO.PathologyMetadataGroupDTO group: groups){
                if (hasDatasetCDE(group.variables(), group.groups())){
                    return true;
                }
            }
        }
        return false;
    }

    private static void updateDatasetCDEEnumerations(
            List<PathologyMetadataDTO.CommonDataElementDTO> variables,
            List<PathologyMetadataDTO.PathologyMetadataGroupDTO> groups,
            List<PathologyDTO.EnumerationDTO> datasetEnumerations
    ){
        if (variables != null) {
            variables.stream().filter(cde -> cde.getCode().equals("dataset")).
                    findAny().ifPresent(cde -> cde.setEnumerations(datasetEnumerations));
        }

        if (groups != null) {
            for (PathologyMetadataDTO.PathologyMetadataGroupDTO group: groups){
                updateDatasetCDEEnumerations(
                        group.variables(),
                        group.groups(),
                        datasetEnumerations
                );
            }
        }
    }

}
