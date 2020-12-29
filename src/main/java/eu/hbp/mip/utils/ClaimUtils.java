package eu.hbp.mip.utils;

import com.google.gson.Gson;
import eu.hbp.mip.models.DTOs.PathologyDTO;
import eu.hbp.mip.utils.Exceptions.UnauthorizedException;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.*;


public class ClaimUtils {

    private static final Gson gson = new Gson();

    public static String allDatasetsAllowedClaim() {
        return "research_dataset_all";
    }

    public static String allExperimentsAllowedClaim() {
        return "research_experiment_all";
    }

    public static String getDatasetClaim(String datasetCode) {
        return "research_dataset_" + datasetCode.toLowerCase();
    }

    public static void validateAccessRightsOnDatasets(Authentication authentication,
                                                      String experimentDatasets, Logger logger) {

        ArrayList<String> authorities = getKeycloakAuthorities(authentication);

        // Don't check for dataset claims if "super" claim exists allowing everything
        if (!hasRoleAccess(authorities, ClaimUtils.allDatasetsAllowedClaim(), logger)) {

            for (String dataset : experimentDatasets.split(",")) {
                String datasetRole = ClaimUtils.getDatasetClaim(dataset);
                if (!hasRoleAccess(authorities, datasetRole, logger)) {
                    logger.LogUserAction("You are not allowed to use dataset: " + dataset);
                    throw new UnauthorizedException("You are not authorized to use these datasets.");
                }
            }
            logger.LogUserAction("User is authorized to use the datasets: " + experimentDatasets);
        }
    }

    public static boolean validateAccessRightsOnExperiments(Authentication authentication, Logger logger) {

        ArrayList<String> authorities = getKeycloakAuthorities(authentication);

        // Check for experiment_all claims
        return  hasRoleAccess(authorities, ClaimUtils.allExperimentsAllowedClaim(), logger);
    }

    public static String getAuthorizedPathologies(Logger logger, Authentication authentication,
                                                  List<PathologyDTO> allPathologies) {
        // --- Providing only the allowed pathologies/datasets to the user  ---
        logger.LogUserAction("Filter out the unauthorised datasets.");

        ArrayList<String> authorities = getKeycloakAuthorities(authentication);

        // If the "dataset_all" claim exists then return everything
        if (hasRoleAccess(authorities, ClaimUtils.allDatasetsAllowedClaim(), logger)) {
            return gson.toJson(allPathologies);
        }

        List<PathologyDTO> userPathologies = new ArrayList<>();
        for (PathologyDTO curPathology : allPathologies) {
            List<PathologyDTO.PathologyDatasetDTO> userPathologyDatasets = new ArrayList<>();
            for (PathologyDTO.PathologyDatasetDTO dataset : curPathology.getDatasets()) {
                if (hasRoleAccess(authorities, ClaimUtils.getDatasetClaim(dataset.getCode()), logger)) {
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

    private static boolean  hasRoleAccess(ArrayList<String> authorities, String role, Logger logger)
    {
        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));

        logger.LogUserAction("User Claims: " + userClaims);
        return userClaims.contains(role.toLowerCase());
    }

    private static ArrayList<String> getKeycloakAuthorities(Authentication authentication){
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) authentication;
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) token.getPrincipal();
        return (ArrayList<String>)keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getOtherClaims().get("authorities");
    }
}
