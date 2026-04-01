package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherResponseDto;
import java.util.List;


public interface TeacherService {


    TeacherResponseDto createTeacher(TeacherRequestDto requestDto);
    List<TeacherResponseDto> getAllTeachers();
    TeacherResponseDto getTeacherById(Long id);
    void deleteTeacherById(Long id);



}
