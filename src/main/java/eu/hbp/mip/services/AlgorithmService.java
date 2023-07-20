package eu.hbp.mip.services;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.hbp.mip.controllers.galaxy.retrofit.RetroFitGalaxyClients;
import eu.hbp.mip.controllers.galaxy.retrofit.RetrofitClientInstance;
import eu.hbp.mip.models.DTOs.ExaremeAlgorithmDTO;
import eu.hbp.mip.models.DTOs.Exareme2AlgorithmDTO;
import eu.hbp.mip.models.galaxy.WorkflowDTO;
import eu.hbp.mip.utils.CustomResourceLoader;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import eu.hbp.mip.utils.HTTPUtil;
import eu.hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static eu.hbp.mip.utils.InputStreamConverter.convertInputStreamToString;

@EnableScheduling
@Service
public class AlgorithmService {

    private static final Gson gson = new Gson();

    private ArrayList<ExaremeAlgorithmDTO> algorithmDTOS = new ArrayList<>();

    @Value("#{'${services.exareme2.algorithmsUrl}'}")
    private String exareme2AlgorithmsUrl;

    @Value("#{'${services.exareme.algorithmsUrl}'}")
    private String exaremeAlgorithmsUrl;

    @Value("#{'${services.galaxy.galaxyUrl}'}")
    private String galaxyUrl;

    @Value("#{'${services.galaxy.galaxyApiKey}'}")
    private String galaxyApiKey;

    @Value("#{'${files.disabledAlgorithms_json}'}")
    private String disabledAlgorithmsFilePath;

    public AlgorithmService(CustomResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    public ArrayList<ExaremeAlgorithmDTO> getAlgorithms() {

        return this.algorithmDTOS;
    }

    public void update(Logger logger) {
        ArrayList<ExaremeAlgorithmDTO> exareme2Algorithms = getExareme2Algorithms(logger);
        ArrayList<ExaremeAlgorithmDTO> exaremeAlgorithms = getExaremeAlgorithms(logger);
        ArrayList<ExaremeAlgorithmDTO> galaxyAlgorithms = getGalaxyWorkflows(logger);
        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();

        // Remove Exareme algorithms that exist in the Exareme2
        if (exareme2Algorithms != null && exaremeAlgorithms != null){
            int old_exareme_algorithm_size = exaremeAlgorithms.size();

            for (ExaremeAlgorithmDTO algorithm : exareme2Algorithms) {
                exaremeAlgorithms.removeIf(obj -> Objects.equals(obj.getName(), algorithm.getName()));
            }
            logger.LogUserAction("Removed "+ (old_exareme_algorithm_size - exaremeAlgorithms.size()) +" deprecated exareme algorithms");
        }

        if (exaremeAlgorithms != null) {
            algorithms.addAll(exaremeAlgorithms);
            logger.LogUserAction("Loaded " + exaremeAlgorithms.size() + " exareme algorithms");
        } else {
            logger.LogUserAction("Fetching exareme algorithms failed");
        }
        if (exareme2Algorithms != null) {
            algorithms.addAll(exareme2Algorithms);
            logger.LogUserAction("Loaded " + exareme2Algorithms.size() + " exareme2 algorithms");
        } else {
            logger.LogUserAction("Fetching exareme2 algorithms failed");
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
            logger.LogUserAction("The disabled algorithms could not be loaded. Exception: " + e.getMessage());
        }

        // Remove any disabled algorithm
        ArrayList<ExaremeAlgorithmDTO> allowedAlgorithms = new ArrayList<>();
        for (ExaremeAlgorithmDTO algorithm : algorithms) {
            if (!disabledAlgorithms.contains(algorithm.getName())) {
                allowedAlgorithms.add(algorithm);
            }
        }

        int algorithmsRemoved = algorithms.size() - allowedAlgorithms.size();
        if (algorithmsRemoved > 0){
            logger.LogUserAction("Removed "+ (algorithmsRemoved) +" disabled algorithms");
        }

        this.algorithmDTOS = allowedAlgorithms;
    }


    @EnableAsync
    public static class AlgorithmAggregator {

        private final AlgorithmService algorithmService;

        public AlgorithmAggregator(AlgorithmService algorithmService){
            this.algorithmService = algorithmService;
        }
        @Async
        @Scheduled(fixedDelayString = "${services.algorithmsUpdateInterval}000")
        public void scheduleFixedRateTaskAsync() throws InterruptedException {
            algorithmService.update(new Logger("AlgorithmAggregator","(GET) /algorithms"));
        }
    }

    public  String getAlgorithmEngineType(String algorithmName){
        Optional<ExaremeAlgorithmDTO> exaremeAlgorithmDTO  = this.algorithmDTOS.stream().filter(algorithmDTO -> algorithmDTO.getName().equals(algorithmName)).findAny();
        if (exaremeAlgorithmDTO.isPresent()) return getAlgorithmEngineType(exaremeAlgorithmDTO.get());
        else throw new BadRequestException("Algorithm: " + algorithmName + " does not exist.");
    }

    private String getAlgorithmEngineType(ExaremeAlgorithmDTO algorithmDTO){
        switch (algorithmDTO.getType()) {
            case "exareme2":
                return "Exareme2";
            case "workflow":
                return "Galaxy";
            default:
                return "Exareme";
        }
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
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        logger.LogUserAction("Completed, returned " + algorithms.size() + " Exareme algorithms.");
        return algorithms;
    }

    /**
     * This method gets all the available exareme2 algorithms and
     *
     * @return a list of AlgorithmDTOs or null if something fails
     */
    public ArrayList<ExaremeAlgorithmDTO> getExareme2Algorithms(Logger logger) {
        ArrayList<Exareme2AlgorithmDTO> exareme2Algorithms;
        // Get Exareme2 algorithms
        try {
            StringBuilder response = new StringBuilder();
            HTTPUtil.sendGet(exareme2AlgorithmsUrl, response);
            exareme2Algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<ArrayList<Exareme2AlgorithmDTO>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.LogUserAction("An exception occurred: " + e.getMessage());
            return null;
        }

        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();
        exareme2Algorithms.forEach(exareme2Algorithm -> algorithms.add(new ExaremeAlgorithmDTO(exareme2Algorithm)));

        logger.LogUserAction("Completed, returned  " + algorithms.size() + " Exareme2 algorithms.");
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