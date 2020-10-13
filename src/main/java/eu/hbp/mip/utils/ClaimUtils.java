package eu.hbp.mip.utils;

import com.google.gson.Gson;
import eu.hbp.mip.model.PathologyDTO;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class ClaimUtils {

    private static final Gson gson = new Gson();

    public static String allDatasetsAllowedClaim() {
        return "dataset_all";
    }

    public static String getDatasetClaim(String datasetCode) {
        return "dataset_" + datasetCode.toLowerCase();
    }

    public static boolean userHasDatasetsAuthorization(String username, Collection<? extends GrantedAuthority> authorities,
                                                       String experimentDatasets) {

        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));
        ActionLogging.LogUserAction(username, "(POST) /experiments/runAlgorithm", userClaims.toString());

        // Don't check for dataset claims if "super" claim exists allowing everything
        if (!userClaims.contains(ClaimUtils.allDatasetsAllowedClaim())) {

            for (String dataset : experimentDatasets.split(",")) {
                String datasetRole = ClaimUtils.getDatasetClaim(dataset);
                if (!userClaims.contains(datasetRole.toLowerCase())) {
                    ActionLogging.LogUserAction(username, "(POST) /experiments/runAlgorithm",
                            "You are not allowed to use dataset: " + dataset);
                    return false;
                }
            }
            ActionLogging.LogUserAction(username, "(POST) /experiments/runAlgorithm",
                    "User is authorized to use the datasets: " + experimentDatasets);
        }
        return true;
    }

    public static String getAuthorizedPathologies(String username, Collection<? extends GrantedAuthority> authorities,
                                                  List<PathologyDTO> allPathologies) {
        // --- Providing only the allowed pathologies/datasets to the user  ---
        ActionLogging.LogUserAction(username,
                "(GET) /pathologies", "Filter out the unauthorised datasets.");

        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));

        ActionLogging.LogUserAction(username,
                "(GET) /pathologies", "User Claims: " + userClaims);

        // If the "dataset_all" claim exists then return everything
        if (userClaims.contains(ClaimUtils.allDatasetsAllowedClaim())) {
            return gson.toJson(allPathologies);
        }

        List<PathologyDTO> userPathologies = new ArrayList<>();
        for (PathologyDTO curPathology : allPathologies) {
            List<PathologyDTO.PathologyDatasetDTO> userPathologyDatasets = new ArrayList<PathologyDTO.PathologyDatasetDTO>();
            for (PathologyDTO.PathologyDatasetDTO dataset : curPathology.getDatasets()) {
                if (userClaims.contains(ClaimUtils.getDatasetClaim(dataset.getCode()))) {
                    ActionLogging.LogUserAction(username , "(GET) /pathologies",
                            "Added dataset: " + dataset.getCode());
                    userPathologyDatasets.add(dataset);
                }
            }

            if (userPathologyDatasets.size() > 0) {
                ActionLogging.LogUserAction(username , "(GET) /pathologies",
                        "Added pathology '" + curPathology.getLabel()
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

}
