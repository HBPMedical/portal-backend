package hbp.mip.utils;

import hbp.mip.pathology.PathologyDTO;
import hbp.mip.utils.Exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        logger.debug("User Claims: " + userClaims);
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
                    logger.warn("You are not allowed to use dataset: " + dataset);
                    throw new UnauthorizedException("You are not authorized to use these datasets.");
                }
            }
            logger.debug("User is authorized to use the datasets: " + experimentDatasets);
        }
    }

    public boolean validateAccessRightsOnALLExperiments(Authentication authentication, Logger logger) {
        ArrayList<String> authorities = getAuthorityRoles(authentication);
        return hasRoleAccess(authorities, allExperimentsAllowedClaim, logger);
    }

    public List<PathologyDTO> getAuthorizedPathologies(Logger logger, Authentication authentication,
                                                       List<PathologyDTO> allPathologies) {

        ArrayList<String> authorities = getAuthorityRoles(authentication);

        if (hasRoleAccess(authorities, allDatasetsAllowedClaim, logger)) {
            return allPathologies;
        }

        List<PathologyDTO> userPathologies = new ArrayList<>();
        for (PathologyDTO curPathology : allPathologies) {
            List<PathologyDTO.EnumerationDTO> userPathologyDatasets = new ArrayList<>();
            Map<String, List<String>> filteredDatasetVariables = new HashMap<>();
            Map<String, List<String>> originalDatasetVariables = curPathology.datasetsVariables();
            for (PathologyDTO.EnumerationDTO dataset : curPathology.datasets()) {
                if (hasRoleAccess(authorities, getDatasetClaim(dataset.code()), logger)) {
                    userPathologyDatasets.add(dataset);
                    List<String> variables = originalDatasetVariables != null
                            ? originalDatasetVariables.get(dataset.code())
                            : null;
                    List<String> safeVariables = variables != null
                            ? Collections.unmodifiableList(new ArrayList<>(variables))
                            : Collections.emptyList();
                    filteredDatasetVariables.put(dataset.code(), safeVariables);
                }
            }

            if (!userPathologyDatasets.isEmpty()) {
                Map<String, List<String>> userDatasetsVariables = filteredDatasetVariables.isEmpty()
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(filteredDatasetVariables);
                PathologyDTO userPathology = new PathologyDTO(
                        curPathology.code(),
                        curPathology.version(),
                        curPathology.label(),
                        curPathology.longitudinal(),
                        curPathology.metadataHierarchy(),
                        userPathologyDatasets,
                        userDatasetsVariables
                );
                userPathologies.add(userPathology);
            }
        }
        return userPathologies;
    }
}
