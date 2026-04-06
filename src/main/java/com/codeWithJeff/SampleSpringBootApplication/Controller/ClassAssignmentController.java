package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Service.ClassAssignmentService;
import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentRequest;
import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-assignments")
@RequiredArgsConstructor
public class ClassAssignmentController {

    private final ClassAssignmentService classAssignmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassAssignmentResponse createAssignment(@Valid @RequestBody ClassAssignmentRequest requestDto) {
        return classAssignmentService.createAssignment(requestDto);
    }

    @GetMapping
    public List<ClassAssignmentResponse> getAllAssignments() {
        return classAssignmentService.getAllAssignments();
    }

    @GetMapping("/{id}")
    public ClassAssignmentResponse getAssignmentById(@PathVariable Long id) {
        return classAssignmentService.getAssignmentById(id);
    }

    @PutMapping("/{id}")
    public ClassAssignmentResponse updateAssignmentById(@PathVariable Long id, @Valid @RequestBody ClassAssignmentRequest requestDto) {
        return classAssignmentService.updateAssignmentById(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignmentById(@PathVariable Long id) {
        classAssignmentService.deleteAssignmentById(id);
    }
}

