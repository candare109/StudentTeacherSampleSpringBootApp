package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.TeacherService;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponseDto createTeacher(@Valid @RequestBody TeacherRequestDto requestDto){
        return teacherService.createTeacher(requestDto);
    }

    @GetMapping("/{id}")
    public TeacherResponseDto getTeacherById(@PathVariable Long id){
        return teacherService.getTeacherById(id);
    }
    @GetMapping
    public List<TeacherResponseDto> getAllTeachers(){
        return teacherService.getAllTeachers();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacherById(@PathVariable Long id){
        teacherService.deleteTeacherById(id);
    }

}
