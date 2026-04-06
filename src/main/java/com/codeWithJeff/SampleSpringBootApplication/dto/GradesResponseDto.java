package com.codeWithJeff.SampleSpringBootApplication.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradesResponseDto {

    private Long gradeId;
    private String studentName;
    private String subject;
    private Double grade;
}
