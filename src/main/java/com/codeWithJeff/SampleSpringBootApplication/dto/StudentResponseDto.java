package com.codeWithJeff.SampleSpringBootApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String course;

}

