package hbp.mip.utils;

import hbp.mip.models.DTOs.exareme2.Exareme2AlgorithmSpecificationDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Exareme2AlgorithmsSpecs {
    private List<Exareme2AlgorithmSpecificationDTO> algorithms = new ArrayList<>();


    public List<Exareme2AlgorithmSpecificationDTO> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(List<Exareme2AlgorithmSpecificationDTO> algorithms) {
        this.algorithms = algorithms;
    }
}
