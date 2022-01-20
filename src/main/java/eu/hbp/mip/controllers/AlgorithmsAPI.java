package eu.hbp.mip.controllers;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.models.DTOs.ExaremeAlgorithmDTO;
import eu.hbp.mip.models.DTOs.MIPEngineAlgorithmDTO;
import eu.hbp.mip.models.galaxy.WorkflowDTO;
import eu.hbp.mip.services.ActiveUserService;
import eu.hbp.mip.utils.CustomResourceLoader;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static eu.hbp.mip.utils.InputStreamConverter.convertInputStreamToString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/algorithms", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/algorithms")
public class AlgorithmsAPI {

    private static final Gson gson = new Gson();

    private final ActiveUserService activeUserService;

    @Value("#{'${services.mipengine.algorithmsUrl}'}")
    private String mipengineAlgorithmsUrl;

    @Value("#{'${services.exareme.algorithmsUrl}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @Value("#{'${files.disabledAlgorithms_json}'}")
    private String disabledAlgorithmsFilePath;

    public AlgorithmsAPI(ActiveUserService activeUserService, CustomResourceLoader resourceLoader) {
        this.activeUserService = activeUserService;
        this.resourceLoader = resourceLoader;
    }

    @ApiOperation(value = "List all algorithms", response = String.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ExaremeAlgorithmDTO>> getAlgorithms() {
        Logger logger = new Logger(activeUserService.getActiveUser().getUsername(), "(GET) /algorithms");

        logger.LogUserAction("Executing...");
        ArrayList<ExaremeAlgorithmDTO> mipengineAlgorithms = getMIPEngineAlgorithms(logger);
        ArrayList<ExaremeAlgorithmDTO> exaremeAlgorithms = getExaremeAlgorithms(logger);
        ArrayList<ExaremeAlgorithmDTO> galaxyAlgorithms = getGalaxyWorkflows(logger);

        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();
        if (exaremeAlgorithms != null) {
            algorithms.addAll(exaremeAlgorithms);
            logger.LogUserAction("Loaded " + exaremeAlgorithms.size() + " exareme algorithms");
        } else {
            logger.LogUserAction("Fetching exareme algorithms failed");
        }
        if (mipengineAlgorithms != null) {
            algorithms.addAll(mipengineAlgorithms);
            logger.LogUserAction("Loaded " + mipengineAlgorithms.size() + " mipengine algorithms");
        } else {
            logger.LogUserAction("Fetching mipengine algorithms failed");
        }
        if (galaxyAlgorithms != null) {
            algorithms.addAll(galaxyAlgorithms);
            logger.LogUserAction("Loaded " + galaxyAlgorithms.size() + " galaxy algorithms");
        } else {
            logger.LogUserAction("Fetching galaxy workflows failed");
        }

        List<String> disabledAlgorithms = new ArrayList<>();
        try {
            disabledAlgorithms = getDisabledAlgorithms();
        } catch (IOException e) {
            logger.LogUserAction("The disabled algorithms could not be loaded.");
        }

        // Remove any disabled algorithm
        ArrayList<ExaremeAlgorithmDTO> allowedAlgorithms = new ArrayList<>();
        for (ExaremeAlgorithmDTO algorithm : algorithms) {
            if (!disabledAlgorithms.contains(algorithm.getName())) {
                allowedAlgorithms.add(algorithm);
            }
        }
        logger.LogUserAction("Successfully listed " + allowedAlgorithms.size() + " algorithms");
        return ResponseEntity.ok(allowedAlgorithms);
    }

    /**
     * This method gets all the available exareme algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public ArrayList<ExaremeAlgorithmDTO> getExaremeAlgorithms(Logger logger) {
        ArrayList<ExaremeAlgorithmDTO> algorithms;
        // Get exareme algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exaremeAlgorithmsUrl, response);
            algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<ArrayList<ExaremeAlgorithmDTO>>() {
                    }.getType()
            );
        } catch (ConnectException e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        logger.LogUserAction("Completed, returned " + algorithms.size() + " algorithms.");
        return algorithms;
    }

    /**
     * This method gets all the available mipengine algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public ArrayList<ExaremeAlgorithmDTO> getMIPEngineAlgorithms(Logger logger) {
        ArrayList<MIPEngineAlgorithmDTO> mipEngineAlgorithms;
        // Get MIPEngine algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(mipengineAlgorithmsUrl, response);
            logger.LogUserAction(response.toString());
            mipEngineAlgorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<ArrayList<MIPEngineAlgorithmDTO>>() {
                    }.getType()
            );
        } catch (ConnectException e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();
        mipEngineAlgorithms.forEach(mipEngineAlgorithm -> algorithms.add(new ExaremeAlgorithmDTO(mipEngineAlgorithm)));

        logger.LogUserAction("Completed, returned " + algorithms.size() + " algorithms.");
        return algorithms;
    }

    /**
     * This method gets all the available galaxy workflows, converts them into algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public ArrayList<ExaremeAlgorithmDTO> getGalaxyWorkflows(Logger logger) {
        List<Workflow> workflowList;
        try {
            // Get all the workflows with the galaxy client
            final GalaxyInstance instance = GalaxyInstanceFactory.get(galaxyUrl, galaxyApiKey);
            final WorkflowsClient workflowsClient = instance.getWorkflowsClient();

            workflowList = new ArrayList<>(workflowsClient.getWorkflows());
        } catch (Exception e) {
            logger.LogUserAction("Error when calling list galaxy workflows: " + e.getMessage());
            return null;
        }

        // Get the workflow details with the custom client to receive them as a WorkflowDTO
        List<WorkflowDTO> workflows = new ArrayList<>();
        // Create the request client
        RetroFitGalaxyClients service = RetrofitClientInstance.getRetrofitInstance().create(RetroFitGalaxyClients.class);
        for (Workflow workflow : workflowList) {
            // Call Galaxy to run the workflow
            Call<WorkflowDTO> call = service.getWorkflowFromGalaxy(workflow.getId(), galaxyApiKey);
            try {
                Response<WorkflowDTO> response = call.execute();

                if (response.code() == 200) {       // Call succeeded
                    workflows.add(response.body());

                } else {     // Something unexpected happened
                    String msgErr = gson.toJson(response.errorBody());
                    logger.LogUserAction("Error Response: " + msgErr);
                    return null;
                }
            } catch (Exception e) {
                logger.LogUserAction("An exception occurred: " + e.getMessage());
                return null;
            }
        }
        logger.LogUserAction("Workflows fetched: " + workflows.size());

        // Convert the workflows to algorithms
        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();
        for (WorkflowDTO workflow : workflows) {
            logger.LogUserAction("Converting workflow: " + workflow);

            algorithms.add(workflow.convertToAlgorithmDTO());

            logger.LogUserAction("Converted algorithm: " + algorithms.get(algorithms.size() - 1));
        }

        logger.LogUserAction("Completed!");
        return algorithms;
    }

    private final CustomResourceLoader resourceLoader;

    /**
     * Fetches the disabled algorithms from a .json file
     *
     * @return a list with their names
     * @throws IOException when the file could not be loaded
     */
    List<String> getDisabledAlgorithms() throws IOException {

        Resource resource = resourceLoader.getResource(disabledAlgorithmsFilePath);

        return gson.fromJson(convertInputStreamToString(
                resource.getInputStream()),
                new TypeToken<List<String>>() {
                }.getType()
        );
    }
}