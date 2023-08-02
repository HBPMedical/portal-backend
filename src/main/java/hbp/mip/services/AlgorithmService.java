package hbp.mip.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hbp.mip.models.DTOs.Exareme2AlgorithmDTO;
import hbp.mip.models.DTOs.ExaremeAlgorithmDTO;
import hbp.mip.utils.CustomResourceLoader;
import hbp.mip.utils.Exceptions.BadRequestException;
import hbp.mip.utils.HTTPUtil;
import hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static hbp.mip.utils.InputStreamConverter.convertInputStreamToString;

@EnableScheduling
@Service
public class AlgorithmService {

    private static final Gson gson = new Gson();
    private final CustomResourceLoader resourceLoader;
    private ArrayList<ExaremeAlgorithmDTO> algorithmDTOS = new ArrayList<>();
    @Value("#{'${services.exareme2.algorithmsUrl}'}")
    private String exareme2AlgorithmsUrl;
    @Value("#{'${services.exareme.algorithmsUrl}'}")
    private String exaremeAlgorithmsUrl;
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
        ArrayList<ExaremeAlgorithmDTO> algorithms = new ArrayList<>();

        // Remove Exareme algorithms that exist in the Exareme2
        if (exareme2Algorithms != null && exaremeAlgorithms != null) {
            int old_exareme_algorithm_size = exaremeAlgorithms.size();

            for (ExaremeAlgorithmDTO algorithm : exareme2Algorithms) {
                exaremeAlgorithms.removeIf(obj -> Objects.equals(obj.getName(), algorithm.getName()));
            }
            logger.LogUserAction("Removed " + (old_exareme_algorithm_size - exaremeAlgorithms.size()) + " deprecated exareme algorithms");
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
        if (algorithmsRemoved > 0) {
            logger.LogUserAction("Removed " + (algorithmsRemoved) + " disabled algorithms");
        }

        this.algorithmDTOS = allowedAlgorithms;
    }

    public String getAlgorithmEngineType(String algorithmName) {
        Optional<ExaremeAlgorithmDTO> exaremeAlgorithmDTO = this.algorithmDTOS.stream().filter(algorithmDTO -> algorithmDTO.getName().equals(algorithmName)).findAny();
        if (exaremeAlgorithmDTO.isPresent()) return getAlgorithmEngineType(exaremeAlgorithmDTO.get());
        else throw new BadRequestException("Algorithm: " + algorithmName + " does not exist.");
    }

    private String getAlgorithmEngineType(ExaremeAlgorithmDTO algorithmDTO) {
        if (algorithmDTO.getType().equals("exareme2")) {
            return "Exareme2";
        }
        return "Exareme";
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

    @EnableAsync
    public static class AlgorithmAggregator {

        private final AlgorithmService algorithmService;

        public AlgorithmAggregator(AlgorithmService algorithmService) {
            this.algorithmService = algorithmService;
        }

        @Async
        @Scheduled(fixedDelayString = "${services.algorithmsUpdateInterval}000")
        public void scheduleFixedRateTaskAsync() {
            algorithmService.update(new Logger("AlgorithmAggregator", "(GET) /algorithms"));
        }
    }
}
