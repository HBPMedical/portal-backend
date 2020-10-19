package eu.hbp.mip.model.DTOs;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AlgorithmDTO {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AlgorithmParamDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<AlgorithmParamDTO> parameters) {
        this.parameters = parameters;
    }

    @SerializedName("name")
    private String name;

    @SerializedName("desc")
    private String desc;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("parameters")
    private List<AlgorithmParamDTO> parameters;

    public static class AlgorithmParamDTO {
        @SerializedName("name")
        private String name;

        @SerializedName("desc")
        private String desc;

        @SerializedName("label")
        private String label;

        @SerializedName("type")
        private String type;

        @SerializedName("columnValuesSQLType")
        private String columnValuesSQLType;

        @SerializedName("columnValuesIsCategorical")
        private String columnValuesIsCategorical;

        @SerializedName("value")
        private String value;

        @SerializedName("defaultValue")
        private String defaultValue;

        @SerializedName("valueType")
        private String valueType;

        @SerializedName("valueNotBlank")
        private String valueNotBlank;

        @SerializedName("valueMultiple")
        private String valueMultiple;

        @SerializedName("valueMin")
        private String valueMin;

        @SerializedName("valueMax")
        private String valueMax;

        @SerializedName("valueEnumerations")
        private List<String> valueEnumerations;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getColumnValuesSQLType() {
            return columnValuesSQLType;
        }

        public void setColumnValuesSQLType(String columnValuesSQLType) {
            this.columnValuesSQLType = columnValuesSQLType;
        }

        public String getColumnValuesIsCategorical() {
            return columnValuesIsCategorical;
        }

        public void setColumnValuesIsCategorical(String columnValuesIsCategorical) {
            this.columnValuesIsCategorical = columnValuesIsCategorical;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getValueType() {
            return valueType;
        }

        public void setValueType(String valueType) {
            this.valueType = valueType;
        }

        public String getValueNotBlank() {
            return valueNotBlank;
        }

        public void setValueNotBlank(String valueNotBlank) {
            this.valueNotBlank = valueNotBlank;
        }

        public String getValueMultiple() {
            return valueMultiple;
        }

        public void setValueMultiple(String valueMultiple) {
            this.valueMultiple = valueMultiple;
        }

        public String getValueMin() {
            return valueMin;
        }

        public void setValueMin(String valueMin) {
            this.valueMin = valueMin;
        }

        public String getValueMax() {
            return valueMax;
        }

        public void setValueMax(String valueMax) {
            this.valueMax = valueMax;
        }

        public List<String> getValueEnumerations() {
            return valueEnumerations;
        }

        public void setValueEnumerations(List<String> valueEnumerations) {
            this.valueEnumerations = valueEnumerations;
        }
    }

}
