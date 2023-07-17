package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ExaremeAlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private List<ExaremeAlgorithmRequestParamDTO> parameters;

    @SerializedName("preprocessing")
    private List<Transformer> preprocessing;

    public ExaremeAlgorithmDTO() {

    }

    public ExaremeAlgorithmDTO(Exareme2AlgorithmDTO exareme2Algorithm) {
        this.name = exareme2Algorithm.getName().toUpperCase();
        this.label = exareme2Algorithm.getLabel();
        this.desc = exareme2Algorithm.getDesc();
        this.type = "exareme2";
        List<ExaremeAlgorithmRequestParamDTO> parameters = new ArrayList<>();
        if (exareme2Algorithm.getInputdata().getY().isPresent()) {
            parameters.add(new ExaremeAlgorithmRequestParamDTO("y", exareme2Algorithm.getInputdata().getY().get()));
        }
        if (exareme2Algorithm.getInputdata().getX().isPresent()) {
            parameters.add(new ExaremeAlgorithmRequestParamDTO("x", exareme2Algorithm.getInputdata().getX().get()));
        }
        parameters.add(new ExaremeAlgorithmRequestParamDTO("pathology", exareme2Algorithm.getInputdata().getData_model()));
        parameters.add(new ExaremeAlgorithmRequestParamDTO("dataset", exareme2Algorithm.getInputdata().getDatasets()));
        parameters.add(new ExaremeAlgorithmRequestParamDTO("filter", exareme2Algorithm.getInputdata().getFilter()));
        if (exareme2Algorithm.getParameters().isPresent()) {
            exareme2Algorithm.getParameters().get().forEach((name, parameterDTO) -> {
                ExaremeAlgorithmRequestParamDTO parameter = new ExaremeAlgorithmRequestParamDTO(name, parameterDTO);
                parameters.add(parameter);
            });
        }
        this.parameters = parameters;
        List<Transformer> preprocessing = new ArrayList<>();
        if (exareme2Algorithm.getPreprocessing().isPresent()) {
            exareme2Algorithm.getPreprocessing().get().forEach(exareme2TransformerDTO -> {
                Transformer transformer = new Transformer(exareme2TransformerDTO);
                preprocessing.add(transformer);
            });
            this.preprocessing = preprocessing;
        }
    }


    @Data
    @AllArgsConstructor
    public static class Transformer {
        @SerializedName("name")
        private String name;

        @SerializedName("desc")
        private String desc;

        @SerializedName("label")
        private String label;

        @SerializedName("parameters")
        private List<ExaremeAlgorithmRequestParamDTO> parameters;


        public Transformer(Exareme2AlgorithmDTO.Exareme2TransformerDTO transformerDTO) {
            this.name = transformerDTO.getName().toUpperCase();
            this.label = transformerDTO.getLabel();
            this.desc = transformerDTO.getDesc();
            List<ExaremeAlgorithmRequestParamDTO> parameters = new ArrayList<>();
            transformerDTO.getParameters().forEach((name, parameterDTO) -> {
                ExaremeAlgorithmRequestParamDTO parameter = new ExaremeAlgorithmRequestParamDTO(name, parameterDTO);
                parameters.add(parameter);
            });
            this.parameters = parameters;
        }
    }

    @Data
    @AllArgsConstructor
    static class Rule {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        @SerializedName("operator")
        private String operator;

        @SerializedName("value")
        private Object value;
    }
}
