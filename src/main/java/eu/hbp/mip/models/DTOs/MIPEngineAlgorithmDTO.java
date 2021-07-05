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

        public ExaremeAlgorithmRequestDTO convertToexaremeAlgorithmRequestDTO(String name) throws Exception {
            ExaremeAlgorithmRequestDTO exaremeAlgorithmRequestDTO = new ExaremeAlgorithmRequestDTO();
            exaremeAlgorithmRequestDTO.setName(name);
            exaremeAlgorithmRequestDTO.setDesc(this.desc);
            exaremeAlgorithmRequestDTO.setValueType(this.type);
            exaremeAlgorithmRequestDTO.setType("other");
            exaremeAlgorithmRequestDTO.setDefaultValue(this.default_value);
            exaremeAlgorithmRequestDTO.setValueNotBlank(this.notblank);
            exaremeAlgorithmRequestDTO.setLabel(this.label);
            exaremeAlgorithmRequestDTO.setValueEnumerations(this.enums);
            exaremeAlgorithmRequestDTO.setValueMultiple(this.multiple);
            exaremeAlgorithmRequestDTO.setValueMin(this.min);
            exaremeAlgorithmRequestDTO.setValueMax(this.max);
            return exaremeAlgorithmRequestDTO;
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

        public List<ExaremeAlgorithmRequestDTO> convertToAlgorithmRequestDTOs() throws Exception {
            List<ExaremeAlgorithmRequestDTO> exaremeAlgorithmRequestDTOS = new ArrayList<>();
            exaremeAlgorithmRequestDTOS.add(this.x.convertToExaremeAlgorithmRequestDTO("x"));
            exaremeAlgorithmRequestDTOS.add(this.y.convertToExaremeAlgorithmRequestDTO("y"));
            exaremeAlgorithmRequestDTOS.add(this.x.convertToExaremeAlgorithmRequestDTO("pathology"));
            exaremeAlgorithmRequestDTOS.add(this.datasets.convertToExaremeAlgorithmRequestDTO("dataset"));
            exaremeAlgorithmRequestDTOS.add(this.filter.convertToExaremeAlgorithmRequestDTO("filter"));
            return exaremeAlgorithmRequestDTOS;
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

        public ExaremeAlgorithmRequestDTO convertToExaremeAlgorithmRequestDTO(String name) throws Exception {
            ExaremeAlgorithmRequestDTO exaremeAlgorithmRequestDTO = new ExaremeAlgorithmRequestDTO();
            exaremeAlgorithmRequestDTO.setName(name);
            exaremeAlgorithmRequestDTO.setDesc(this.desc);
            exaremeAlgorithmRequestDTO.setValue("");
            exaremeAlgorithmRequestDTO.setValueNotBlank(this.notblank);
            exaremeAlgorithmRequestDTO.setValueMultiple(this.multiple);
            String[] hidden = {"x","y","dataset", "filter","pathology","centers","formula"};
            exaremeAlgorithmRequestDTO.setLabel(Arrays.asList(hidden).contains(exaremeAlgorithmRequestDTO.getName()) ? exaremeAlgorithmRequestDTO.getName():this.label);
            if(name.equals("dataset") || name.equals("filter") || name.equals("pathology")){
                exaremeAlgorithmRequestDTO.setValueType(this.types.get(0));
                exaremeAlgorithmRequestDTO.setType(exaremeAlgorithmRequestDTO.getName());
            }
            else{
                exaremeAlgorithmRequestDTO.setType("column");
                exaremeAlgorithmRequestDTO.setColumnValuesSQLType(String.join(", ", this.types));
                exaremeAlgorithmRequestDTO.setColumnValuesIsCategorical(getColumnValuesIsCategorical(this.stattypes));
            }
            return exaremeAlgorithmRequestDTO;
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

    public ExaremeAlgorithmDTO convertToAlgorithmDTO()
    {
        ExaremeAlgorithmDTO exaremeAlgorithmDTO = new ExaremeAlgorithmDTO();
        exaremeAlgorithmDTO.setName(this.name.toUpperCase());
        exaremeAlgorithmDTO.setLabel(this.label);
        exaremeAlgorithmDTO.setDesc(this.desc);
        exaremeAlgorithmDTO.setType("mipengine");
        List<ExaremeAlgorithmRequestDTO> parameters = new ArrayList<>();
        try {
            parameters.addAll(this.inputdata.convertToAlgorithmRequestDTOs());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.parameters.forEach((name, parameterDTO) -> {
            try {
                ExaremeAlgorithmRequestDTO parameter = parameterDTO.convertToexaremeAlgorithmRequestDTO(name);
                parameters.add(parameter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        exaremeAlgorithmDTO.setParameters(parameters);
        return exaremeAlgorithmDTO;
    }
}
