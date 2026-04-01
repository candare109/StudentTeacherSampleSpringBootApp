package com.codeWithJeff.SampleSpringBootApplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder

public class SubjectDto {
    //Response - null on request
    private Long subjectId;

    @NotNull
    private Long studentId;

    @NotNull
    private Long teacherId;

    //both on request and response
    @NotBlank
    private String subject;

    //response
    private String studentName;

    private String teacherName;


}
