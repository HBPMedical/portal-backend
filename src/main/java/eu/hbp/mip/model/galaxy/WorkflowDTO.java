package eu.hbp.mip.model.galaxy;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import eu.hbp.mip.model.AlgorithmDTO;

import java.util.*;

public class WorkflowDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("inputs")
    private HashMap<String, WorkflowInputDTO> inputs;

    @SerializedName("steps")
    private HashMap<String, WorkflowStepDTO> steps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, WorkflowInputDTO> getInputs() {
        return inputs;
    }

    public void setInputs(HashMap<String, WorkflowInputDTO> inputs) {
        this.inputs = inputs;
    }

    public HashMap<String, WorkflowStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(HashMap<String, WorkflowStepDTO> steps) {
        this.steps = steps;
    }

    public class WorkflowInputDTO {
        @SerializedName("uuid")
        private String uuid;

        @SerializedName("label")
        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public class WorkflowStepDTO {
        @SerializedName("id")
        private int id;

        @SerializedName("type")
        private String type;

        @SerializedName("annotation")
        private String annotation;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getAnnotation() {
            return annotation;
        }

        public void setAnnotation(String annotation) {
            this.annotation = annotation;
        }
    }

    public AlgorithmDTO convertToAlgorithmDTO() {

        AlgorithmDTO algorithmDTO = new AlgorithmDTO();

        // Transfer workflow information
        algorithmDTO.setName(id);
        algorithmDTO.setDesc("");
        algorithmDTO.setLabel(name);
        algorithmDTO.setType("workflow");

        // Transfer workflow parameters information
        List<AlgorithmDTO.AlgorithmParamDTO> algorithmParams = new LinkedList<>();
        Gson gson = new Gson();
        for (Map.Entry<String, WorkflowInputDTO> workflowInput : getInputs().entrySet()) {

            // Convert the annotation to algorithm Parameter
            AlgorithmDTO.AlgorithmParamDTO algorithmParam;
            if (steps.get(workflowInput.getKey()).getAnnotation() != null) {
                algorithmParam = gson.fromJson(steps.get(workflowInput.getKey()).getAnnotation(),
                        AlgorithmDTO.AlgorithmParamDTO.class);
            } else {
                // If annotation is not provided, auto-fill some information
                algorithmParam = new AlgorithmDTO.AlgorithmParamDTO();
                // When the constraints are not known, set the most relaxed constraints
                algorithmParam.setDesc("");
                algorithmParam.setValue("");
                algorithmParam.setValueType("string");
                algorithmParam.setValueNotBlank("false");
                algorithmParam.setDefaultValue("");
                algorithmParam.setDefaultValue("true");
                // If label is dataset/pathology/filter/formula the type should be the same
                if (workflowInput.getValue().getLabel().equals("dataset") ||
                        workflowInput.getValue().getLabel().equals("pathology") ||
                        workflowInput.getValue().getLabel().equals("filter") ||
                        workflowInput.getValue().getLabel().equals("formula")) {
                    algorithmParam.setType(workflowInput.getValue().getLabel());
                } else if (workflowInput.getValue().getLabel().equals("x") ||
                        workflowInput.getValue().getLabel().equals("y")) {
                    algorithmParam.setType("column");
                    algorithmParam.setColumnValuesSQLType("text,real,integer");
                    algorithmParam.setColumnValuesIsCategorical("");
                } else {
                    algorithmParam.setType("other");
                }
            }
            // Set the name to the workflow id
            algorithmParam.setName(workflowInput.getValue().getUuid());
            algorithmParam.setLabel(workflowInput.getValue().getLabel());

            algorithmParams.add(algorithmParam);
        }
        algorithmDTO.setParameters(algorithmParams);

        return algorithmDTO;
    }

}
