package hbp.mip.algorithm;

import com.google.gson.JsonSyntaxException;
import hbp.mip.experiment.ExperimentExecutionDTO;
import hbp.mip.utils.Exceptions.InternalServerError;
import hbp.mip.utils.JsonConverters;

import java.util.*;

public record Exareme2AlgorithmRequestDTO(
        String request_id,
        Exareme2InputDataRequestDTO inputdata,
        Map<String, Object> parameters,
        Map<String, Object> preprocessing,
        String type
) {

    public static Exareme2AlgorithmRequestDTO create(
            UUID experimentUUID,
            List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO> exaremeAlgorithmRequestParamDTOs,
            List<ExperimentExecutionDTO.AlgorithmExecutionDTO.TransformerExecutionDTO> exaremeTransformers,
            Exareme2AlgorithmSpecificationDTO exareme2AlgorithmSpecificationDTO) {

        // List of inputDataFields
        List<String> inputDataFields = Arrays.asList("pathology", "dataset", "x", "y", "filter");

        // Create lists to hold the separated DTOs
        List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO> inputDataDTOs = new ArrayList<>();
        List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO> parametersDTOs = new ArrayList<>();

        // Split the DTOs into the respective lists
        for (ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO dto : exaremeAlgorithmRequestParamDTOs) {
            if (inputDataFields.contains(dto.name())) {
                inputDataDTOs.add(dto);
            } else {
                parametersDTOs.add(dto);
            }
        }

        // Call the constructor with the separated lists
        return new Exareme2AlgorithmRequestDTO(
                experimentUUID.toString(),
                getInputData(inputDataDTOs),
                getParameters(parametersDTOs, exareme2AlgorithmSpecificationDTO),
                getPreprocessing(exaremeTransformers, exareme2AlgorithmSpecificationDTO),
                "exareme2"
        );
    }

    private static Exareme2InputDataRequestDTO getInputData(List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO> exaremeAlgorithmRequestParamDTOs) {
        if (exaremeAlgorithmRequestParamDTOs == null) {
            return null;
        }

        Map<String, String> parameters = new HashMap<>();
        for (var parameter : exaremeAlgorithmRequestParamDTOs) {
            parameters.put(parameter.name(), parameter.value());
        }

        // ATTENTION:
        // pathology and dataset fields are mandatory and should always exist in the parameters.
        // x, y, filter fields are optional.

        String data_model = parameters.get("pathology");

        List<String> datasets = Arrays.asList(parameters.get("dataset").split(","));

        List<String> x = null;
        if (parameters.containsKey("x") && parameters.get("x") != null)
            x = Arrays.asList(parameters.get("x").split(","));

        List<String> y = null;
        if (parameters.containsKey("y") && parameters.get("y") != null)
            y = Arrays.asList(parameters.get("y").split(","));

        Exareme2FilterRequestDTO filters = null;
        if (parameters.containsKey("filter") && parameters.get("filter") != null && !parameters.get("filter").isEmpty())
            filters = JsonConverters.convertJsonStringToObject(parameters.get("filter"), Exareme2AlgorithmRequestDTO.Exareme2FilterRequestDTO.class);

        return new Exareme2AlgorithmRequestDTO.Exareme2InputDataRequestDTO(
                data_model,
                datasets,
                x,
                y,
                filters
        );
    }

    private static Map<String, Object> getParameters(List<ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO> exaremeAlgorithmRequestParamDTOs, Exareme2AlgorithmSpecificationDTO exareme2AlgorithmSpecificationDTO) {
        if (exaremeAlgorithmRequestParamDTOs == null) {
            return null;
        }

        HashMap<String, Object> exareme2Parameters = new HashMap<>();
        exaremeAlgorithmRequestParamDTOs.forEach(parameter -> {
            Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmParameterSpecificationDTO paramSpecDto = exareme2AlgorithmSpecificationDTO.parameters().get(parameter.name());
            if (paramSpecDto == null){
                throw new InternalServerError("Parameter " + parameter.name() + " not found in algorithm:" + exareme2AlgorithmSpecificationDTO.name());
            }
            exareme2Parameters.put(parameter.name(), convertStringToProperExareme2ParameterTypeAccordingToSpecs(parameter.value(), paramSpecDto));
        });
        return exareme2Parameters;
    }

    private static Map<String, Object> getPreprocessing(List<ExperimentExecutionDTO.AlgorithmExecutionDTO.TransformerExecutionDTO> exaremeTransformers, Exareme2AlgorithmSpecificationDTO exareme2AlgorithmSpecificationDTO) {
        if (exaremeTransformers == null) {
            return null;
        }

        HashMap<String, Object> exareme2Preprocessing = new HashMap<>();
        exaremeTransformers.forEach(transformer -> {
            String transformer_name = transformer.name();
            HashMap<String, Object> transformerParameterDTOs = new HashMap<>();
            for (ExperimentExecutionDTO.AlgorithmExecutionDTO.AlgorithmParameterExecutionDTO parameter : transformer.parameters()){
                String param_name = parameter.name();
                Optional<Exareme2AlgorithmSpecificationDTO.Exareme2TransformerSpecificationDTO> transformerSpecificationDTO = exareme2AlgorithmSpecificationDTO.preprocessing().stream()
                        .filter(transformerSpec-> transformerSpec.name().equals(transformer_name))
                        .findFirst();
                if (transformerSpecificationDTO.isEmpty()) throw new InternalServerError("Missing the transformer: " + transformer_name);

                Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmParameterSpecificationDTO paramSpecDto = transformerSpecificationDTO.get().parameters().get(param_name);
                if (paramSpecDto == null){
                    throw new InternalServerError("Parameter " + parameter.name() + " not found in transformer:" + transformerSpecificationDTO.get().name());
                }
                transformerParameterDTOs.put(param_name, convertStringToProperExareme2ParameterTypeAccordingToSpecs(parameter.value(), paramSpecDto));
            }
            exareme2Preprocessing.put(transformer_name, transformerParameterDTOs);
        });

        return exareme2Preprocessing;
    }

    private static Object convertStringToProperExareme2ParameterTypeAccordingToSpecs(String value, Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmParameterSpecificationDTO paramSpecDto) {
        if (paramSpecDto.enums() != null){
            return value;
        }
        return convertStringToProperExareme2ParameterType(value);
    }

    private static Object convertStringToProperExareme2ParameterType(String str) {
        if (isMap(str))
            return JsonConverters.convertJsonStringToObject(str, Map.class);

        String[] stringValues = str.split(",");
        if (stringValues.length == 0)
            return "";

        if (stringValues.length == 1)
            return convertStringToProperType(stringValues[0]);

        List<Object> values = new ArrayList<>();
        for (String value : stringValues) {
            values.add(convertStringToProperType(value));
        }

        return values;
    }

    private static Object convertStringToProperType(String str) {
        if (isInteger(str))
            return Integer.parseInt(str);
        else if (isFloat(str))
            return Double.parseDouble(str);
        else
            return str;
    }

    private static boolean isFloat(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean isMap(String strMap) {
        if (strMap == null) {
            return false;
        }
        try {
            JsonConverters.convertJsonStringToObject(strMap, Map.class);
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }

    public record Exareme2InputDataRequestDTO(String data_model, List<String> datasets, List<String> x, List<String> y,
                                              Exareme2FilterRequestDTO filters) {
    }

    public record Exareme2FilterRequestDTO(String condition, List<Object> rules) {
    }


}
