package hbp.mip.algorithm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
public class AlgorithmsSpecs {
    private List<AlgorithmSpecificationDTO> algorithms = new ArrayList<>();
}
