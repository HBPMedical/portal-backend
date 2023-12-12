package hbp.mip.algorithm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hbp.mip.utils.CustomResourceLoader;
import hbp.mip.utils.HTTPUtil;
import hbp.mip.utils.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static hbp.mip.utils.InputStreamConverter.convertInputStreamToString;

@Service
public class AlgorithmService {

    private static final Gson gson = new Gson();

    private final Exareme2AlgorithmsSpecs exareme2AlgorithmsSpecs;
    private final CustomResourceLoader resourceLoader;

    @Value("${files.disabledAlgorithms_json}")
    private String disabledAlgorithmsFilePath;

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
    private List<Exareme2AlgorithmSpecificationDTO> getExareme2Algorithms(Logger logger) {
        List<Exareme2AlgorithmSpecificationDTO> algorithms;
        StringBuilder response = new StringBuilder();
        try {
            HTTPUtil.sendGet(exareme2AlgorithmsUrl, response);
            algorithms = gson.fromJson(
                    response.toString(),
                    new TypeToken<List<Exareme2AlgorithmSpecificationDTO>>() {
                    }.getType()
            );
        } catch (Exception e) {
            logger.error("Could not fetch exareme2 algorithms: " + e.getMessage());
            return Collections.emptyList();
        }

        logger.debug("Fetched " + algorithms.size() + " exareme2 algorithms.");
        exareme2AlgorithmsSpecs.setAlgorithms(algorithms);
        return algorithms;
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
