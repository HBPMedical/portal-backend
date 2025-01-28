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

    @Value("${services.exareme2.attributesUrl}")
    private String exareme2AttributesUrl;

    @Value("${services.exareme2.cdesMetadataUrl}")
    private String exareme2CDEsMetadataUrl;

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
        Map<String, DataModelAttributes> exareme2DataModelAttributes;
        Type pathologyAttributesType = new TypeToken<Map<String, DataModelAttributes>>(){}.getType();
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2AttributesUrl, response);
            exareme2DataModelAttributes = JsonConverters.convertJsonStringToObject(response.toString(),pathologyAttributesType);
        } catch (Exception e) {
            logger.error("Could not fetch exareme2 dataModels' metadata: " + e.getMessage());
            throw new InternalServerError(e.getMessage());
        }
        List<DataModelDTO> dataModelDTOs = new ArrayList<>();
        exareme2DataModelAttributes.forEach((pathology, attributes) -> {
            assert attributes.properties != null;
            assert attributes.properties.get("cdes") != null;
            assert !attributes.properties.get("cdes").isEmpty();
            DataModelDTO dataModel = attributes.properties.get("cdes").get(0);
            DataModelDTO dataModelWithDatasets = dataModel.withDatasets();

            dataModelDTOs.add(dataModelWithDatasets);
        });
        return dataModelDTOs;
    }
    record DataModelAttributes(Map<String, List<DataModelDTO>> properties, List<String> tags){}

}
