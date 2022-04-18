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

    public ExaremeAlgorithmDTO() {

    }

    public ExaremeAlgorithmDTO(MIPEngineAlgorithmDTO mipEngineAlgorithm) {
        this.name = mipEngineAlgorithm.getName().toUpperCase();
        this.label = mipEngineAlgorithm.getLabel();
        this.desc = mipEngineAlgorithm.getDesc();
        this.type = "mipengine";
        List<ExaremeAlgorithmRequestParamDTO> parameters = new ArrayList<>();
        if (mipEngineAlgorithm.getInputdata().getY().isPresent()) {
            parameters.add(new ExaremeAlgorithmRequestParamDTO("y", mipEngineAlgorithm.getInputdata().getY().get()));
        }
        if (mipEngineAlgorithm.getInputdata().getX().isPresent()) {
            parameters.add(new ExaremeAlgorithmRequestParamDTO("x", mipEngineAlgorithm.getInputdata().getX().get()));
        }
        parameters.add(new ExaremeAlgorithmRequestParamDTO("pathology", mipEngineAlgorithm.getInputdata().getData_model()));
        parameters.add(new ExaremeAlgorithmRequestParamDTO("dataset", mipEngineAlgorithm.getInputdata().getDatasets()));
        parameters.add(new ExaremeAlgorithmRequestParamDTO("filter", mipEngineAlgorithm.getInputdata().getFilter()));
        if (mipEngineAlgorithm.getParameters().isPresent()) {
            mipEngineAlgorithm.getParameters().get().forEach((name, parameterDTO) -> {
                ExaremeAlgorithmRequestParamDTO parameter = new ExaremeAlgorithmRequestParamDTO(name, parameterDTO);
                parameters.add(parameter);
            });
        }
        this.setParameters(parameters);
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
