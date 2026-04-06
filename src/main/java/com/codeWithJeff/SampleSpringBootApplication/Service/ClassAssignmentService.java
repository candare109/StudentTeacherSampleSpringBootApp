package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentRequest;
import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentResponse;

import java.util.List;

public interface ClassAssignmentService {
    ClassAssignmentResponse createAssignment(ClassAssignmentRequest requestDto);
    List<ClassAssignmentResponse> getAllAssignments();
    ClassAssignmentResponse getAssignmentById(Long id);
    ClassAssignmentResponse updateAssignmentById(Long id, ClassAssignmentRequest requestDto);
    void deleteAssignmentById(Long id);
}

