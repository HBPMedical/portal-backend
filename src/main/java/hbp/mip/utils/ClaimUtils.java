package hbp.mip.utils;

import hbp.mip.datamodel.DataModelDTO;
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
                                               List<String> experimentDatasets, Logger logger) {

        ArrayList<String> authorities = getAuthorityRoles(authentication);

        if (!hasRoleAccess(authorities, allDatasetsAllowedClaim, logger)) {
            for (String dataset : experimentDatasets) {
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

    public List<DataModelDTO> getAuthorizedDataModels(Logger logger, Authentication authentication,
                                                      List<DataModelDTO> allDataModels) {

        ArrayList<String> authorities = getAuthorityRoles(authentication);

        if (hasRoleAccess(authorities, allDatasetsAllowedClaim, logger)) {
            return allDataModels;
        }

        List<DataModelDTO> userDataModels = new ArrayList<>();
        for (DataModelDTO curDataModel : allDataModels) {
            List<DataModelDTO.EnumerationDTO> dataModelDatasets = curDataModel.datasets();
            if (dataModelDatasets == null || dataModelDatasets.isEmpty()) {
                continue;
            }

            List<DataModelDTO.EnumerationDTO> userDataModelDatasets = new ArrayList<>();
            Map<String, List<String>> filteredDatasetVariables = new HashMap<>();
            Map<String, List<String>> originalDatasetVariables = curDataModel.datasetsVariables();

            for (DataModelDTO.EnumerationDTO dataset : dataModelDatasets) {
                if (!hasRoleAccess(authorities, getDatasetClaim(dataset.code()), logger)) {
                    continue;
                }

                userDataModelDatasets.add(dataset);
                if (originalDatasetVariables == null) {
                    continue;
                }

                List<String> variables = originalDatasetVariables.get(dataset.code());
                List<String> safeVariables = variables != null
                        ? Collections.unmodifiableList(new ArrayList<>(variables))
                        : Collections.emptyList();
                filteredDatasetVariables.put(dataset.code(), safeVariables);
            }

            if (userDataModelDatasets.isEmpty()) {
                continue;
            }

            List<DataModelDTO.EnumerationDTO> safeDatasets = Collections.unmodifiableList(new ArrayList<>(userDataModelDatasets));
            Map<String, List<String>> userDatasetsVariables = filteredDatasetVariables.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(filteredDatasetVariables);

            DataModelDTO userDataModel = new DataModelDTO(
                    curDataModel.code(),
                    curDataModel.version(),
                    curDataModel.label(),
                    curDataModel.longitudinal(),
                    curDataModel.variables(),
                    curDataModel.groups(),
                    safeDatasets,
                    userDatasetsVariables
            );
            userDataModels.add(userDataModel);
        }
        return userDataModels;
    }
}
