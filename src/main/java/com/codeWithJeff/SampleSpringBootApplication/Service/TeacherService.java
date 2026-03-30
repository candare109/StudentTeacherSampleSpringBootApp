package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherResponseDto;


public interface TeacherService {


    TeacherResponseDto createTeacher(TeacherRequestDto requestDto);
    TeacherResponseDto getTeacherById(Long id);

}
