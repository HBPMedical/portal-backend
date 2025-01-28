package hbp.mip.algorithm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hbp.mip.utils.CustomResourceLoader;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static hbp.mip.utils.InputStreamConverter.convertInputStreamToString;

@Service
@EnableScheduling
public class AlgorithmService {

    private static final Gson gson = new Gson();

    private final AlgorithmsSpecs algorithmsSpecs;
    private final CustomResourceLoader resourceLoader;

    @Value("${files.disabledAlgorithms_json}")
    private String disabledAlgorithmsFilePath;

    @Value("${services.exareme2.algorithmsUrl}")
    private String exareme2AlgorithmsUrl;

    public AlgorithmService(AlgorithmsSpecs algorithmsSpecs, CustomResourceLoader resourceLoader) {
        this.algorithmsSpecs = algorithmsSpecs;
        this.resourceLoader = resourceLoader;
    }

    public List<AlgorithmSpecificationDTO> getAlgorithms(Logger logger) {

        ArrayList<AlgorithmSpecificationDTO> exaremeAlgorithms = (ArrayList<AlgorithmSpecificationDTO>) getExareme2Algorithms(logger);


        List<String> disabledAlgorithms = getDisabledAlgorithms(logger);
        logger.debug("Disabled algorithms: " + disabledAlgorithms);

        // Remove any disabled algorithm
        ArrayList<AlgorithmSpecificationDTO> enabledAlgorithms = new ArrayList<>();
        for (AlgorithmSpecificationDTO algorithm : exaremeAlgorithms) {
            if (!disabledAlgorithms.contains(algorithm.name())) {
                enabledAlgorithms.add(algorithm);
            }
        }

        logger.debug("Disabled " + (exaremeAlgorithms.size() - enabledAlgorithms.size()) + " algorithms.");
        return enabledAlgorithms;
    }

    /**
     * This method gets all the available exareme2 algorithms and removes the disabled.
     *
     * @return a list of Exareme2AlgorithmSpecificationDTO or null if something fails
     */
    private List<AlgorithmSpecificationDTO> getExareme2Algorithms(Logger logger) {
        List<AlgorithmSpecificationDTO> algorithms;
        StringBuilder response = new StringBuilder();
        try {
            HTTPUtil.sendGet(exareme2AlgorithmsUrl, response);
            algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<List<AlgorithmSpecificationDTO>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.error("Could not fetch exareme2 algorithms: " + e.getMessage());
            return Collections.emptyList();
        }

        // Filter out algorithms with type "flower"
        algorithms = algorithms.stream()
                .filter(algorithm -> "exareme2".equals(algorithm.type()))
                .collect(Collectors.toList());
        logger.debug("Fetched " + algorithms.size() + " exareme2 algorithms.");
        algorithmsSpecs.setAlgorithms(algorithms);
        return algorithms;
    }

    @EnableAsync
    public static class AlgorithmAggregator {

        private final AlgorithmService algorithmService;

        public AlgorithmAggregator(AlgorithmService algorithmService){
            this.algorithmService = algorithmService;
        }
        @Async
        @Scheduled(fixedDelayString = "${services.algorithmsUpdateInterval}000")
        public void scheduleFixedRateTaskAsync() {
            algorithmService.getExareme2Algorithms(new Logger("AlgorithmAggregator","(GET) /algorithms"));
        }
    }

    /**
     * Fetches the disabled algorithms from a .json file
     *
     * @return a list with their names
     */
    private List<String> getDisabledAlgorithms(Logger logger) {
        Resource resource = resourceLoader.getResource(disabledAlgorithmsFilePath);
        try {
            return gson.fromJson(
                    convertInputStreamToString(resource.getInputStream()),
                    new TypeToken<List<String>>() {
                    }.getType()
            );
        } catch (IOException e) {
            logger.error("Could not load the disabled algorithms. Exception: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
