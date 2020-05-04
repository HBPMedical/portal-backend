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
        return "dataset_" + datasetCode;
    }

    public static boolean userHasDatasetsAuthorization(String username, Collection<? extends GrantedAuthority> authorities,
                                                       String experimentDatasets) {

        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));
        UserActionLogging.LogUserAction(username, "User Claims", userClaims.toString());

        // Don't check for dataset claims if "super" claim exists allowing everything
        if (!userClaims.contains(ClaimUtils.allDatasetsAllowedClaim())) {

            for (String dataset : experimentDatasets.split(",")) {
                String datasetRole = ClaimUtils.getDatasetClaim(dataset);
                if (!userClaims.contains(datasetRole.toLowerCase())) {
                    UserActionLogging.LogUserAction(username, "Run algorithm",
                            "You are not allowed to use dataset: " + dataset);
                    return false;
                }
            }
            UserActionLogging.LogUserAction(username, "Run algorithm",
                    "User is authorized to use the datasets: " + experimentDatasets);
        }
        return true;
    }

    public static String getAuthorizedPathologies(String username, Collection<? extends GrantedAuthority> authorities,
                                                  List<PathologyDTO> allPathologies) {
        // --- Providing only the allowed pathologies/datasets to the user  ---
        UserActionLogging.LogUserAction(username,
                "Load pathologies", "Filter out the unauthorised datasets.");

        List<String> userClaims = Arrays.asList(authorities.toString().toLowerCase()
                .replaceAll("[\\s+\\]\\[]", "").split(","));

        UserActionLogging.LogUserAction(username,
                "Load pathologies", "User Claims: " + userClaims);

        // If the "dataset_all" claim exists then return everything
        if (userClaims.contains(ClaimUtils.allDatasetsAllowedClaim())) {
            return gson.toJson(allPathologies);
        }

        List<PathologyDTO> userPathologies = new ArrayList<>();
        for (PathologyDTO curPathology : allPathologies) {
            UserActionLogging.LogUserAction(username,
                    "Load pathologies", "Checking pathology: " + curPathology.getCode());

            List<PathologyDTO.PathologyDatasetDTO> userPathologyDatasets = new ArrayList<PathologyDTO.PathologyDatasetDTO>();
            for (PathologyDTO.PathologyDatasetDTO dataset : curPathology.getDatasets()) {
                if (userClaims.contains(ClaimUtils.getDatasetClaim(dataset.getCode()))) {
                    UserActionLogging.LogUserAction(username, "Load pathologies",
                            "Added dataset: " + dataset.getCode());
                    userPathologyDatasets.add(dataset);
                }else{
                    UserActionLogging.LogUserAction(username, "Load pathologies",
                            "Dataset not added: " + dataset.getCode());
                    UserActionLogging.LogUserAction(username, "Load pathologies",
                            "Claim did not exist: " + ClaimUtils.getDatasetClaim(dataset.getCode()));
                }
            }

            if (userPathologyDatasets.size() > 0) {
                UserActionLogging.LogUserAction(username, "Load pathologies",
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
