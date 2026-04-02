package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Service.SubjectService;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subject")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectDto createSubject(@Valid @RequestBody SubjectDto requestSubjectDto){
        return subjectService.createSubject(requestSubjectDto);
    }
    @GetMapping
    public List<SubjectDto> getAllSubjects(){
        return subjectService.getAllSubjects();
    }
}
