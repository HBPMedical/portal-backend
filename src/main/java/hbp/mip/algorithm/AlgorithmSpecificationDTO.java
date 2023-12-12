package hbp.mip.algorithm;

import hbp.mip.utils.Exceptions.InternalServerError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public record AlgorithmSpecificationDTO(
        String name,
        String label,
        String desc,
        List<AlgorithmParameterSpecificationDTO> parameters,
        List<TransformerSpecificationDTO> preprocessing
){
    public record TransformerSpecificationDTO(String name, String label, String desc,
                                              List<AlgorithmParameterSpecificationDTO> parameters) {
        public TransformerSpecificationDTO(Exareme2AlgorithmSpecificationDTO.Exareme2TransformerSpecificationDTO transformerDTO) {
            this(
                    transformerDTO.name(),
                    transformerDTO.label(),
                    transformerDTO.desc(),
                    getAlgorithmParameterSpecifications(transformerDTO.parameters())
            );
        }

        private static List<AlgorithmParameterSpecificationDTO> getAlgorithmParameterSpecifications(
                Map<String, Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmParameterSpecificationDTO> exareme2Parameters
        ) {
            List<AlgorithmParameterSpecificationDTO> parameters = new ArrayList<>();
            exareme2Parameters.forEach((name, parameterDTO)
                    -> parameters.add(new AlgorithmParameterSpecificationDTO(name, parameterDTO)));
            return parameters;
        }

    }

    public record AlgorithmParameterSpecificationDTO(
            String name,
            String label,
            String desc,
            String type,
            String columnValuesSQLType,
            String columnValuesIsCategorical,
            String defaultValue,
            String valueType,
            String valueNotBlank,
            String valueMultiple,
            String valueMin,
            String valueMax,
            List<String> valueEnumerations
    ) {
        public AlgorithmParameterSpecificationDTO(String name, Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmParameterSpecificationDTO parameter) {
            this(
                    name,
                    parameter.label(),
                    parameter.desc(),
                    "other",
                    "",
                    "",
                    parameter.default_value(),
                    parameter.types().get(0),
                    parameter.notblank(),
                    parameter.multiple(),
                    parameter.min(),
                    parameter.max(),
                    parameter.enums() != null ? parameter.enums().source() : null
            );
        }

        public AlgorithmParameterSpecificationDTO(String name, Exareme2AlgorithmSpecificationDTO.Exareme2AlgorithmInputDataDetailSpecificationDTO inputDataDetail) {
            this(
                    name,
                    inputDataDetail.label(),
                    inputDataDetail.desc(),
                    getParameterType(name),
                    getParameterColumnValuesSQLType(name, inputDataDetail.types()),
                    getParameterColumnValuesIsCategorical(name, inputDataDetail.stattypes()),
                    "",
                    getParameterValueType(name, inputDataDetail.types()),
                    inputDataDetail.notblank(),
                    inputDataDetail.multiple(),
                    "",
                    "",
                    null
            );
        }

        private static String getParameterType(String inputDataDetailName){
            if (inputDataDetailName.equals("dataset") || inputDataDetailName.equals("filter") || inputDataDetailName.equals("pathology")) {
                return inputDataDetailName;
            }
            return "column";
        }

        private static String getParameterValueType(String inputDataDetailName, List<String> types){
            if (inputDataDetailName.equals("dataset") || inputDataDetailName.equals("filter") || inputDataDetailName.equals("pathology")) {
                return types.get(0);
            }
            return "";
        }

        private static String getParameterColumnValuesSQLType(String inputDataDetailName, List<String> types){
            if (inputDataDetailName.equals("dataset") || inputDataDetailName.equals("filter") || inputDataDetailName.equals("pathology")) {
                return "";
            }
            return String.join(", ", types);
        }

        private static String getParameterColumnValuesIsCategorical(String inputDataDetailName, List<String> stattypes){
            if (inputDataDetailName.equals("dataset") || inputDataDetailName.equals("filter") || inputDataDetailName.equals("pathology")) {
                return "";
            }

            if (stattypes.contains("nominal") && stattypes.contains("numerical")) {
                return "";
            } else if (stattypes.contains("nominal")) {
                return "true";
            } else if (stattypes.contains("numerical")) {
                return "false";
            } else {
                throw new InternalServerError("Invalid stattypes: " + stattypes);
            }
        }
    }

    public AlgorithmSpecificationDTO(Exareme2AlgorithmSpecificationDTO exareme2Algorithm){
        this(
                exareme2Algorithm.name(),
                exareme2Algorithm.label(),
                exareme2Algorithm.desc(),
                getAlgorithmParameters(exareme2Algorithm),
                getTransformers(exareme2Algorithm.preprocessing())
        );
    }

    private static List<AlgorithmParameterSpecificationDTO> getAlgorithmParameters(Exareme2AlgorithmSpecificationDTO exareme2Algorithm){
        List<AlgorithmParameterSpecificationDTO> parameters = new ArrayList<>();
        if (exareme2Algorithm.inputdata().y() != null) {
            parameters.add(new AlgorithmParameterSpecificationDTO("y", exareme2Algorithm.inputdata().y()));
        }
        if (exareme2Algorithm.inputdata().x() != null) {
            parameters.add(new AlgorithmParameterSpecificationDTO("x", exareme2Algorithm.inputdata().x()));
        }
        parameters.add(new AlgorithmParameterSpecificationDTO("pathology", exareme2Algorithm.inputdata().data_model()));
        parameters.add(new AlgorithmParameterSpecificationDTO("dataset", exareme2Algorithm.inputdata().datasets()));
        parameters.add(new AlgorithmParameterSpecificationDTO("filter", exareme2Algorithm.inputdata().filter()));
        exareme2Algorithm.parameters().forEach((name, parameterDTO)
                -> parameters.add(new AlgorithmParameterSpecificationDTO(name, parameterDTO)));
        return parameters;
    }

    private static List<TransformerSpecificationDTO> getTransformers(List<Exareme2AlgorithmSpecificationDTO.Exareme2TransformerSpecificationDTO> exareme2Transformers){
        List<TransformerSpecificationDTO> preprocessing = new ArrayList<>();
        exareme2Transformers.forEach(exareme2TransformerSpecificationDTO
                -> preprocessing.add(new TransformerSpecificationDTO(exareme2TransformerSpecificationDTO)));
        return preprocessing;
    }

}
