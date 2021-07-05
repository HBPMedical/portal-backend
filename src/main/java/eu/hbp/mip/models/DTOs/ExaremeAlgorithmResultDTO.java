package eu.hbp.mip.models.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ExaremeAlgorithmResultDTO {
    private int code;
    private List<Object> results;
}


