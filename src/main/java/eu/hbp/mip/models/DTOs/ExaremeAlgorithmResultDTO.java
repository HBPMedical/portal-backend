package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExaremeAlgorithmResultDTO {
    private int code;
    private List<Object> result;
}


