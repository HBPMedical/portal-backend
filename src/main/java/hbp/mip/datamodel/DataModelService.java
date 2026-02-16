package hbp.mip.datamodel;

import com.google.gson.reflect.TypeToken;
import hbp.mip.utils.ClaimUtils;
import hbp.mip.utils.Exceptions.InternalServerError;
import hbp.mip.utils.HTTPUtil;
import hbp.mip.utils.JsonConverters;
import hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;

@Service
public class DataModelService {

    private final ClaimUtils claimUtils;

    @Value("${authentication.enabled}")
    private boolean authenticationIsEnabled;

    @Value("${services.exaflow.attributesUrl}")
    private String exaflowAttributesUrl;

    @Value("${services.exaflow.datasets_variables}")
    private String exaflowDatasetsVariables;

    @Value("${services.exaflow.cdesMetadataUrl}")
    private String exaflowCDEsMetadataUrl;

    public DataModelService(ClaimUtils claimUtils) {
        this.claimUtils = claimUtils;
    }

    public List<DataModelDTO> getDataModels(Authentication authentication, Logger logger) {

        List<DataModelDTO> allDataModelDTOS = getAggregatedDataModelDTOs(logger);

        if (!authenticationIsEnabled) {
            return allDataModelDTOS;
        }
        return claimUtils.getAuthorizedDataModels(logger, authentication, allDataModelDTOS);
    }

    private List<DataModelDTO> getAggregatedDataModelDTOs(Logger logger) {
        Map<String, DataModelAttributes> exaflowDataModelAttributes;
        Map<String, Map<String, List<String>>> datasetsVariablesByDataModel;
        Map<String, List<DataModelDTO.EnumerationDTO>> datasetEnumerationsByDataModel;
        Type pathologyAttributesType = new TypeToken<Map<String, DataModelAttributes>>(){}.getType();
        Type datasetsVariablesType = new TypeToken<Map<String, Map<String, List<String>>>>(){}.getType();
        Type cdesMetadataType = new TypeToken<Map<String, Map<String, CDEMetadata>>>(){}.getType();

        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaflowAttributesUrl, response);
            exaflowDataModelAttributes = JsonConverters.convertJsonStringToObject(response.toString(), pathologyAttributesType);
        } catch (Exception e) {
            logger.error("Could not fetch exaflow dataModels' metadata: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }

        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaflowDatasetsVariables, response);
            Map<String, Map<String, List<String>>> convertedResponse = JsonConverters.convertJsonStringToObject(response.toString(), datasetsVariablesType);
            datasetsVariablesByDataModel = convertedResponse != null ? convertedResponse : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Could not fetch exaflow datasets variables: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }

        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaflowCDEsMetadataUrl, response);
            Map<String, Map<String, CDEMetadata>> convertedResponse = JsonConverters.convertJsonStringToObject(response.toString(), cdesMetadataType);
            datasetEnumerationsByDataModel = convertedResponse != null ? extractDatasetEnumerations(convertedResponse) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Could not fetch exaflow datasets availability: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }

        List<DataModelDTO> dataModelDTOs = new ArrayList<>();
        exaflowDataModelAttributes.forEach((pathology, attributes) -> {
            assert attributes.properties != null;
            assert attributes.properties.get("cdes") != null;
            assert !attributes.properties.get("cdes").isEmpty();
            DataModelDTO dataModel = attributes.properties.get("cdes").get(0);

            Map<String, List<String>> datasetVariables = selectDatasetVariables(datasetsVariablesByDataModel, dataModel);
            List<DataModelDTO.EnumerationDTO> datasetEnumerations = selectDatasetEnumerations(datasetEnumerationsByDataModel, dataModel);
            List<DataModelDTO.EnumerationDTO> datasets = datasetEnumerations != null
                    ? datasetEnumerations
                    : dataModel.datasets();

            dataModelDTOs.add(new DataModelDTO(
                    dataModel.code(),
                    dataModel.version(),
                    dataModel.label(),
                    dataModel.longitudinal(),
                    dataModel.variables(),
                    dataModel.groups(),
                    datasets,
                    datasetVariables
            ));
        });
        return dataModelDTOs;
    }

    private Map<String, List<String>> selectDatasetVariables(Map<String, Map<String, List<String>>> datasetVariablesByDataModel, DataModelDTO dataModel) {
        if (datasetVariablesByDataModel == null || dataModel == null) {
            return Collections.emptyMap();
        }

        String versionedKey = dataModel.code() + ":" + dataModel.version();
        Map<String, List<String>> variables = datasetVariablesByDataModel.get(versionedKey);
        if (variables != null) {
            return variables;
        }

        variables = datasetVariablesByDataModel.get(dataModel.code());
        return variables != null ? variables : Collections.emptyMap();
    }

    private List<DataModelDTO.EnumerationDTO> selectDatasetEnumerations(Map<String, List<DataModelDTO.EnumerationDTO>> datasetEnumerationsByDataModel, DataModelDTO dataModel) {
        if (datasetEnumerationsByDataModel == null || dataModel == null) {
            return null;
        }

        String versionedKey = dataModel.code() + ":" + dataModel.version();
        List<DataModelDTO.EnumerationDTO> enumerations = datasetEnumerationsByDataModel.get(versionedKey);
        if (enumerations != null) {
            return enumerations;
        }

        return datasetEnumerationsByDataModel.get(dataModel.code());
    }

    private Map<String, List<DataModelDTO.EnumerationDTO>> extractDatasetEnumerations(Map<String, Map<String, CDEMetadata>> cdesMetadataByDataModel) {
        Map<String, List<DataModelDTO.EnumerationDTO>> datasetEnumerations = new HashMap<>();

        cdesMetadataByDataModel.forEach((dataModel, cdes) -> {
            if (cdes == null || !cdes.containsKey("dataset")) {
                return;
            }

            CDEMetadata datasetCDE = cdes.get("dataset");
            if (datasetCDE == null || datasetCDE.enumerations == null || datasetCDE.enumerations.isEmpty()) {
                datasetEnumerations.put(dataModel, Collections.emptyList());
                return;
            }

            List<DataModelDTO.EnumerationDTO> enumerations = datasetCDE.enumerations.entrySet().stream()
                    .map(entry -> new DataModelDTO.EnumerationDTO(entry.getKey(), entry.getValue()))
                    .toList();
            datasetEnumerations.put(dataModel, Collections.unmodifiableList(enumerations));
        });

        return datasetEnumerations;
    }

    record DataModelAttributes(Map<String, List<DataModelDTO>> properties, List<String> tags){}

    record CDEMetadata(String code, Map<String, String> enumerations){}

}
