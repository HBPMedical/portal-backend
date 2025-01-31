package hbp.mip.algorithm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hbp.mip.utils.CustomResourceLoader;
import hbp.mip.utils.HTTPUtil;
import hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class AlgorithmService {

    private static final Gson gson = new Gson();

    private final Exareme2AlgorithmsSpecs exareme2AlgorithmsSpecs;
    private final CustomResourceLoader resourceLoader;


    @Value("${services.exareme2.algorithmsUrl}")
    private String exareme2AlgorithmsUrl;

    public AlgorithmService(Exareme2AlgorithmsSpecs exareme2AlgorithmsSpecs, CustomResourceLoader resourceLoader) {
        this.exareme2AlgorithmsSpecs = exareme2AlgorithmsSpecs;
        this.resourceLoader = resourceLoader;
    }

    public List<AlgorithmSpecificationDTO> getAlgorithms(Logger logger) {
        // Fetch exareme2 algorithm specifications and convert to generic algorithm specifications.
        ArrayList<AlgorithmSpecificationDTO> exaremeAlgorithms = new ArrayList<>();
        getExareme2Algorithms(logger).forEach(algorithm -> exaremeAlgorithms.add(new AlgorithmSpecificationDTO(algorithm)));
        return exaremeAlgorithms;
    }

    /**
     * This method gets all the available exareme2 algorithms.
     *
     * @return a list of Exareme2AlgorithmSpecificationDTO or an empty list if something fails
     */
    private List<Exareme2AlgorithmSpecificationDTO> getExareme2Algorithms(Logger logger) {
        List<Exareme2AlgorithmSpecificationDTO> algorithms;
        StringBuilder response = new StringBuilder();

        // Create structured request details
        Map<String, Object> requestDetails = Map.of(
                "method", "GET",
                "endpoint", exareme2AlgorithmsUrl
        );

        logger.info("Fetching Exareme2 algorithms", requestDetails);

        try {
            HTTPUtil.sendGet(exareme2AlgorithmsUrl, response);
            algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<List<Exareme2AlgorithmSpecificationDTO>>() {}.getType()
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
        exareme2AlgorithmsSpecs.setAlgorithms(algorithms);
        return algorithms;
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
            Map<String, Object> requestDetails = Map.of(
                    "method", "GET",
                    "endpoint", "/algorithms (scheduled fetch)"
            );

            var logger = new Logger("AlgorithmAggregator", requestDetails);
            algorithmService.getExareme2Algorithms(logger);
        }
    }
}
