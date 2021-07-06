package eu.hbp.mip.models.DTOs;

import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.utils.Exceptions.InternalServerError;
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
    private InputdataDTO inputdata;

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

        public ExaremeAlgorithmRequestParamDTO convertToexaremeAlgorithmRequestDTO(String name){
            ExaremeAlgorithmRequestParamDTO exaremeAlgorithmRequestParamDTO = new ExaremeAlgorithmRequestParamDTO();
            exaremeAlgorithmRequestParamDTO.setName(name);
            exaremeAlgorithmRequestParamDTO.setDesc(this.desc);
            exaremeAlgorithmRequestParamDTO.setValueType(this.type);
            exaremeAlgorithmRequestParamDTO.setType("other");
            exaremeAlgorithmRequestParamDTO.setDefaultValue(this.default_value);
            exaremeAlgorithmRequestParamDTO.setValueNotBlank(this.notblank);
            exaremeAlgorithmRequestParamDTO.setLabel(this.label);
            exaremeAlgorithmRequestParamDTO.setValueEnumerations(this.enums);
            exaremeAlgorithmRequestParamDTO.setValueMultiple(this.multiple);
            exaremeAlgorithmRequestParamDTO.setValueMin(this.min);
            exaremeAlgorithmRequestParamDTO.setValueMax(this.max);
            return exaremeAlgorithmRequestParamDTO;
        }
    }

    @Getter
    @Setter
    public static class InputdataDTO {
        @SerializedName("x")
        private InputDataDetailDTO x;

        @SerializedName("y")
        private InputDataDetailDTO y;

        @SerializedName("pathology")
        private InputDataDetailDTO pathology;

        @SerializedName("datasets")
        private InputDataDetailDTO datasets;

        @SerializedName("filter")
        private InputDataDetailDTO filter;

        public List<ExaremeAlgorithmRequestParamDTO> convertToAlgorithmRequestParamDTOs(){
            List<ExaremeAlgorithmRequestParamDTO> exaremeAlgorithmRequestParamDTOS = new ArrayList<>();
            exaremeAlgorithmRequestParamDTOS.add(this.x.convertToExaremeAlgorithmRequestDTO("x"));
            exaremeAlgorithmRequestParamDTOS.add(this.y.convertToExaremeAlgorithmRequestDTO("y"));
            exaremeAlgorithmRequestParamDTOS.add(this.pathology.convertToExaremeAlgorithmRequestDTO("pathology"));
            exaremeAlgorithmRequestParamDTOS.add(this.datasets.convertToExaremeAlgorithmRequestDTO("dataset"));
            exaremeAlgorithmRequestParamDTOS.add(this.filter.convertToExaremeAlgorithmRequestDTO("filter"));
            return exaremeAlgorithmRequestParamDTOS;
        }
    }

    @Getter
    @Setter
    public static class InputDataDetailDTO {

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

        public ExaremeAlgorithmRequestParamDTO convertToExaremeAlgorithmRequestDTO(String name){
            ExaremeAlgorithmRequestParamDTO exaremeAlgorithmRequestParamDTO = new ExaremeAlgorithmRequestParamDTO();
            exaremeAlgorithmRequestParamDTO.setName(name);
            exaremeAlgorithmRequestParamDTO.setDesc(this.desc);
            exaremeAlgorithmRequestParamDTO.setValue("");
            exaremeAlgorithmRequestParamDTO.setValueNotBlank(this.notblank);
            exaremeAlgorithmRequestParamDTO.setValueMultiple(this.multiple);
            String[] hidden = {"x","y","dataset", "filter","pathology","centers","formula"};
            exaremeAlgorithmRequestParamDTO.setLabel(Arrays.asList(hidden).contains(exaremeAlgorithmRequestParamDTO.getName()) ? exaremeAlgorithmRequestParamDTO.getName():this.label);
            if(name.equals("dataset") || name.equals("filter") || name.equals("pathology")){
                exaremeAlgorithmRequestParamDTO.setValueType(this.types.get(0));
                exaremeAlgorithmRequestParamDTO.setType(exaremeAlgorithmRequestParamDTO.getName());
            }
            else{
                exaremeAlgorithmRequestParamDTO.setType("column");
                exaremeAlgorithmRequestParamDTO.setColumnValuesSQLType(String.join(", ", this.types));
                exaremeAlgorithmRequestParamDTO.setColumnValuesIsCategorical(getColumnValuesIsCategorical(this.stattypes));
            }
            return exaremeAlgorithmRequestParamDTO;
        }

        private String getColumnValuesIsCategorical(List<String> stattypes){

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
                throw new InternalServerError("Invalid stattypes");
            }
        }
    }

    public ExaremeAlgorithmDTO convertToAlgorithmDTO()
    {
        ExaremeAlgorithmDTO exaremeAlgorithmDTO = new ExaremeAlgorithmDTO();
        exaremeAlgorithmDTO.setName(this.name.toUpperCase());
        exaremeAlgorithmDTO.setLabel(this.label);
        exaremeAlgorithmDTO.setDesc(this.desc);
        exaremeAlgorithmDTO.setType("mipengine");
        List<ExaremeAlgorithmRequestParamDTO> parameters = new ArrayList<>(this.inputdata.convertToAlgorithmRequestParamDTOs());
        this.parameters.forEach((name, parameterDTO) -> {
            ExaremeAlgorithmRequestParamDTO parameter = parameterDTO.convertToexaremeAlgorithmRequestDTO(name);
            parameters.add(parameter);
        });
        exaremeAlgorithmDTO.setParameters(parameters);
        return exaremeAlgorithmDTO;
    }
}
