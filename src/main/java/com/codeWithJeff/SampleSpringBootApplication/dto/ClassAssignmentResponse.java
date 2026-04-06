package com.codeWithJeff.SampleSpringBootApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassAssignmentResponse {

    private Long classAssignmentId;
    private String studentName;
    private String subjectName;
    private String teacherName;

}