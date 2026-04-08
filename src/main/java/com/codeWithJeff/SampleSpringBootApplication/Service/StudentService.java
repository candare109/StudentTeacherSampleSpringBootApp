package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.StudentRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.StudentResponseDto;


import java.util.List;


public interface StudentService {

    StudentResponseDto createStudent(StudentRequestDto requestDto);

    List<StudentResponseDto> getAllStudents();

    StudentResponseDto getStudentById(Long id);

    StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto);

    void deleteStudent(Long id);
}

