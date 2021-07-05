package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.JsonConverters;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

@Getter
@Setter
public class MIPEngineAlgorithmDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private Hashtable<String, ParameterDTO> parameters;

    @SerializedName("crossvalidation")
    private String crossvalidation;

    @SerializedName("inputdata")
    private Hashtable<String, InputDataParamDTO> inputdata;

    @Getter
    @Setter
    public static class ParameterDTO {

        @SerializedName("label")
        private String label;

        @SerializedName("notblank")
        private String notblank;

        @SerializedName("multiple")
        private String multiple;

        @SerializedName("types")
        private String type;

        @SerializedName("desc")
        private String desc;

        @SerializedName("min")
        private String min;

        @SerializedName("max")
        private String max;

        @SerializedName("default_value")
        private String default_value;

        @SerializedName("enums")
        private List<String> enums;

        public AlgorithmDTO.AlgorithmParamDTO convertToAlgorithmParamDTO(String name) throws Exception {
            AlgorithmDTO.AlgorithmParamDTO algorithmParamDTO = new AlgorithmDTO.AlgorithmParamDTO();
            algorithmParamDTO.setName(name);
            algorithmParamDTO.setDesc(this.desc);
            algorithmParamDTO.setValueType(this.type);
            algorithmParamDTO.setType("other");
            algorithmParamDTO.setDefaultValue(this.default_value);
            algorithmParamDTO.setValueNotBlank(this.notblank);
            algorithmParamDTO.setLabel(this.label);
            algorithmParamDTO.setValueEnumerations(this.enums);
            algorithmParamDTO.setValueMultiple(this.multiple);
            algorithmParamDTO.setValueMin(this.min);
            algorithmParamDTO.setValueMax(this.max);
            return algorithmParamDTO;
        }
    }

    @Getter
    @Setter
    public static class InputDataParamDTO {

        @SerializedName("stattypes")
        private List<String> stattypes;

        @SerializedName("label")
        private String label;

        @SerializedName("notblank")
        private String notblank;

        @SerializedName("enumslen")
        private Integer enumslen;

        @SerializedName("multiple")
        private String multiple;

        @SerializedName("types")
        private List<String> types;

        @SerializedName("desc")
        private String desc;

        public AlgorithmDTO.AlgorithmParamDTO convertToAlgorithmParamDTO(String name) throws Exception {
            AlgorithmDTO.AlgorithmParamDTO algorithmParamDTO = new AlgorithmDTO.AlgorithmParamDTO();
            algorithmParamDTO.setName(name);
            algorithmParamDTO.setDesc(this.desc);

            if(name.equals("datasets") || name.equals("filter") || name.equals("pathology")){
                if(name.equals("datasets")){
                    algorithmParamDTO.setName("dataset");
                }
                algorithmParamDTO.setValueType(this.types.get(0));
                algorithmParamDTO.setType(algorithmParamDTO.getName());
            }
            else{
                algorithmParamDTO.setType("column");
                algorithmParamDTO.setColumnValuesSQLType(String.join(", ", this.types));
                algorithmParamDTO.setColumnValuesIsCategorical(getColumnValuesIsCategorical(this.stattypes));
            }
            algorithmParamDTO.setValue("");
            algorithmParamDTO.setValueNotBlank(this.notblank);
            algorithmParamDTO.setValueMultiple(this.multiple);
            String[] hidden = {"x","y","dataset", "filter","pathology","centers","formula"};
            algorithmParamDTO.setLabel(Arrays.asList(hidden).contains(algorithmParamDTO.getName()) ? algorithmParamDTO.getName():this.label);
            System.out.println("-------------------------------------------------------->");
            System.out.println(JsonConverters.convertObjectToJsonString(algorithmParamDTO));
            return algorithmParamDTO;
        }
        private String getColumnValuesIsCategorical(List<String> stattypes) throws Exception {

            if (stattypes.contains("nominal") && stattypes.contains("numerical")){
                return "";
            }
            else if (stattypes.contains("nominal")){
                return "true";
            }
            else if (stattypes.contains("numerical")){
                return "false";
            }
            else{
                throw new Exception("Invalid stattypes");
            }
        }
    }

    public AlgorithmDTO convertToAlgorithmDTO()
    {
        AlgorithmDTO algorithmDTO = new AlgorithmDTO();
        algorithmDTO.setName(this.name.toUpperCase());
        algorithmDTO.setLabel(this.label);
        algorithmDTO.setDesc(this.desc);
        algorithmDTO.setType("mipengine");
        List<AlgorithmDTO.AlgorithmParamDTO> parameters = new ArrayList<>();
        this.inputdata.forEach((name, inputDataParamDTO) -> {
            try {
                AlgorithmDTO.AlgorithmParamDTO parameter = inputDataParamDTO.convertToAlgorithmParamDTO(name);
                parameters.add(parameter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        this.parameters.forEach((name, parameterDTO) -> {
            try {
                AlgorithmDTO.AlgorithmParamDTO parameter = parameterDTO.convertToAlgorithmParamDTO(name);
                parameters.add(parameter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        algorithmDTO.setParameters(parameters);
        return algorithmDTO;
    }
}
