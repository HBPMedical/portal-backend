package eu.hbp.mip.utils;

import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.utils.Exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClaimUtils {

    @Value("${authentication.all_datasets_allowed_claim}")
    private String allDatasetsAllowedClaim;

    @Value("${authentication.all_experiments_allowed_claim}")
    private String allExperimentsAllowedClaim;

    @Value("${authentication.dataset_claim_prefix}")
    private String datasetClaimPrefix;

    private String getDatasetClaim(String datasetCode) {
        return datasetClaimPrefix + datasetCode.toLowerCase();
    }

    private static boolean hasRoleAccess(ArrayList<String> authorities, String role, Logger logger) {
        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));

        logger.LogUserAction("User Claims: " + userClaims);
        return userClaims.contains(role.toLowerCase());
    }

    private static ArrayList<String> getAuthorityRoles(Authentication authentication) {
        return (ArrayList<String>) authentication.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public void validateAccessRightsOnDatasets(Authentication authentication,
                                                      String experimentDatasets, Logger logger) {

        ArrayList<String> authorities = getAuthorityRoles(authentication);

        // Don't check for dataset claims if "super" claim exists allowing everything
        if (!hasRoleAccess(authorities, allDatasetsAllowedClaim, logger)) {

            for (String dataset : experimentDatasets.split(",")) {
                String datasetRole = getDatasetClaim(dataset);
                if (!hasRoleAccess(authorities, datasetRole, logger)) {
                    logger.LogUserAction("You are not allowed to use dataset: " + dataset);
                    throw new UnauthorizedException("You are not authorized to use these datasets.");
                }
            }
            logger.LogUserAction("User is authorized to use the datasets: " + experimentDatasets);
        }
    }

    public boolean validateAccessRightsOnALLExperiments(Authentication authentication, Logger logger) {
        ArrayList<String> authorities = getAuthorityRoles(authentication);
        return hasRoleAccess(authorities, allExperimentsAllowedClaim, logger);
    }

    public List<PathologyDTO> getAuthorizedPathologies(Logger logger, Authentication authentication,
                                                              List<PathologyDTO> allPathologies) {

        ArrayList<String> authorities = getAuthorityRoles(authentication);

        List<PathologyDTO> userPathologies = new ArrayList<>();
        if (hasRoleAccess(authorities, allDatasetsAllowedClaim, logger)) {
            userPathologies = allPathologies;

        } else {
            for (PathologyDTO curPathology : allPathologies) {
                List<PathologyDTO.EnumerationDTO> userPathologyDatasets = new ArrayList<>();
                for (PathologyDTO.EnumerationDTO dataset : curPathology.getDatasets()) {
                    if (hasRoleAccess(authorities, getDatasetClaim(dataset.getCode()), logger)) {
                        userPathologyDatasets.add(dataset);
                    }
                }

                if (userPathologyDatasets.size() > 0) {
                    PathologyDTO userPathology = new PathologyDTO();
                    userPathology.setCode(curPathology.getCode());
                    userPathology.setLabel(curPathology.getLabel());
                    userPathology.setMetadataHierarchyDTO(curPathology.getMetadataHierarchyDTO());
                    userPathology.setDatasets(userPathologyDatasets);
                    userPathologies.add(userPathology);
                }
            }
        }

        String userPathologiesSTR = userPathologies.stream().map(PathologyDTO::toString)
                .collect(Collectors.joining(", "));
        logger.LogUserAction("Allowed pathologies: [" + userPathologiesSTR + "]");
        return userPathologies;
    }
}
