package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectResponseDto;

import java.util.List;

public interface SubjectService {
    SubjectResponseDto createSubject(SubjectRequestDto requestDto);
    List<SubjectResponseDto> getAllSubjects();
    SubjectResponseDto getSubjectById(Long id);
    SubjectResponseDto updateSubjectById(Long id, SubjectRequestDto requestDto);
    void deleteSubjectById(Long id);
}
