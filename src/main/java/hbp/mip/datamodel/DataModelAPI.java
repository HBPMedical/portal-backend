package hbp.mip.datamodel;

import hbp.mip.user.ActiveUserService;
import hbp.mip.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/data-models")
public class DataModelAPI {

    private final DataModelService dataModelService;

    private final ActiveUserService activeUserService;

    public DataModelAPI(ActiveUserService activeUserService, DataModelService dataModelService) {
        this.activeUserService = activeUserService;
        this.dataModelService = dataModelService;
    }

    @GetMapping
    public ResponseEntity<List<DataModelDTO>> getDataModels(Authentication authentication) {
        var logger = new Logger(activeUserService.getActiveUser(authentication).username(), "(GET) /dataModels");
        logger.info("Request for dataModels.");
        var dataModels = dataModelService.getDataModels(authentication, logger);

        String userDataModelsSTR = dataModels.stream().map(DataModelDTO::code)
                .collect(Collectors.joining(", "));
        logger.info("DataModels returned: " + dataModels.size() + ". [" + userDataModelsSTR + "].");

        return ResponseEntity.ok(dataModels);
    }

}
