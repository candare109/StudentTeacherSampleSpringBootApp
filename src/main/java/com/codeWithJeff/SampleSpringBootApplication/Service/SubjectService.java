package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectDto;

import java.util.List;

public interface SubjectService {
    SubjectDto createSubject(SubjectDto requestSubjectDto);
    List<SubjectDto> getAllSubjects();

}
