package eu.hbp.mip.utils;

import com.google.gson.Gson;
import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.utils.Exceptions.UnauthorizedException;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;


public class ClaimUtils {

    private static final Gson gson = new Gson();

    public static String allDatasetsAllowedClaim() {
        return "role_research_dataset_all";
    }

    public static String allExperimentsAllowedClaim() {
        return "role_research_experiment_all";
    }

    public static String getDatasetClaim(String datasetCode) {
        return "role_research_dataset_" + datasetCode.toLowerCase();
    }

    public static void validateAccessRightsOnDatasets(String username, Collection<? extends GrantedAuthority> authorities,
                                                       String experimentDatasets, Logger logger) {

        // Don't check for dataset claims if "super" claim exists allowing everything
        if (!hasRoleAccess(username, authorities, ClaimUtils.allDatasetsAllowedClaim(), logger)) {

            for (String dataset : experimentDatasets.split(",")) {
                String datasetRole = ClaimUtils.getDatasetClaim(dataset);
                if (!hasRoleAccess(username, authorities, datasetRole, logger)) {
                    logger.LogUserAction("You are not allowed to use dataset: " + dataset);
                    throw new UnauthorizedException("You are not authorized to use these datasets.");
                }
            }
            logger.LogUserAction("User is authorized to use the datasets: " + experimentDatasets);
        }
    }

    public static boolean validateAccessRightsOnExperiments(String username, Collection<? extends GrantedAuthority> authorities, Logger logger) {

        // Check for experiment_all claims
        return  hasRoleAccess(username, authorities, ClaimUtils.allExperimentsAllowedClaim(), logger);
    }

    public static String getAuthorizedPathologies(String username, Logger logger, Collection<? extends GrantedAuthority> authorities,
                                                  List<PathologyDTO> allPathologies) {
        // --- Providing only the allowed pathologies/datasets to the user  ---
        logger.LogUserAction("Filter out the unauthorised datasets.");

        // If the "dataset_all" claim exists then return everything
        if (hasRoleAccess(username, authorities, ClaimUtils.allDatasetsAllowedClaim(), logger)) {
            return gson.toJson(allPathologies);
        }

        List<PathologyDTO> userPathologies = new ArrayList<>();
        for (PathologyDTO curPathology : allPathologies) {
            List<PathologyDTO.PathologyDatasetDTO> userPathologyDatasets = new ArrayList<>();
            for (PathologyDTO.PathologyDatasetDTO dataset : curPathology.getDatasets()) {
                if (hasRoleAccess(username, authorities, ClaimUtils.getDatasetClaim(dataset.getCode()), logger)) {
                    logger.LogUserAction("Added dataset: " + dataset.getCode());
                    userPathologyDatasets.add(dataset);
                }
            }

            if (userPathologyDatasets.size() > 0) {
                logger.LogUserAction("Added pathology '" + curPathology.getLabel()
                                + "' with datasets: '" + userPathologyDatasets + "'");

                PathologyDTO userPathology = new PathologyDTO();
                userPathology.setCode(curPathology.getCode());
                userPathology.setLabel(curPathology.getLabel());
                userPathology.setMetadataHierarchy(curPathology.getMetadataHierarchy());
                userPathology.setDatasets(userPathologyDatasets);
                userPathologies.add(userPathology);
            }
        }

        return gson.toJson(userPathologies);
    }

    private static boolean  hasRoleAccess(String username, Collection<? extends GrantedAuthority> authorities,String role, Logger logger)
    {
        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));

        logger.LogUserAction("User Claims: " + userClaims);
        return userClaims.contains(role.toLowerCase());
    }
}
