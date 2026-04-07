package com.codeWithJeff.SampleSpringBootApplication.Service;



import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import com.codeWithJeff.SampleSpringBootApplication.dto.GradesRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.GradesResponseDto;

import java.util.List;

public interface GradeService {
    GradesResponseDto createGrades(GradesRequestDto gradesRequestDto);
    GradesResponseDto getGradesById(Long id);
    List<GradesResponseDto> getAllGrades();
    double getAverageGradeByStudentId(Long studentId);
}
