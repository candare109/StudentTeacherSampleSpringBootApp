package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Service.SubjectService;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponseDto createSubject(@Valid @RequestBody SubjectRequestDto requestDto) {
        return subjectService.createSubject(requestDto);
    }

    @GetMapping
    public List<SubjectResponseDto> getAllSubjects() {
        return subjectService.getAllSubjects();
    }

    @GetMapping("/{id}")
    public SubjectResponseDto getSubjectById(@PathVariable Long id) {
        return subjectService.getSubjectById(id);
    }

    @PutMapping("/{id}")
    public SubjectResponseDto updateSubjectById(@PathVariable Long id, @Valid @RequestBody SubjectRequestDto requestDto) {
        return subjectService.updateSubjectById(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubjectById(@PathVariable Long id) {
        subjectService.deleteSubjectById(id);
    }
}
