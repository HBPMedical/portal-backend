package eu.hbp.mip.model;

import java.util.List;

public class ExperimentExecutionDTO {

    private String name;
    private String model;
    private List<AlgorithmExecutionDTO> algorithms;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<AlgorithmExecutionDTO> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(List<AlgorithmExecutionDTO> algorithms) {
        this.algorithms = algorithms;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class AlgorithmExecutionDTO {

        private String name;
        private String label;
        private String type;

        private List<AlgorithmExecutionParamDTO> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<AlgorithmExecutionDTO.AlgorithmExecutionParamDTO> getParameters() {
            return parameters;
        }

        public void setParameters(List<AlgorithmExecutionDTO.AlgorithmExecutionParamDTO> parameters) {
            this.parameters = parameters;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public static class AlgorithmExecutionParamDTO {

            private String name;
            private String label;
            private String value;

            public String getName() {
                return this.name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public String getLabel() {
                return label;
            }

            public void setLabel(String label) {
                this.label = label;
            }
        }
    }
}
