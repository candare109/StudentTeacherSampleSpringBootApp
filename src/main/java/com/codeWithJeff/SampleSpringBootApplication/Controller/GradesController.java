package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.dto.GradesRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.GradesResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.codeWithJeff.SampleSpringBootApplication.Service.GradeService;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradesController {
    private final GradeService gradesService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public GradesResponseDto createGrades(@RequestBody GradesRequestDto gradesRequestDto){
     return gradesService.createGrades(gradesRequestDto);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.FOUND)
    public GradesResponseDto getGradesById(@PathVariable Long id){
        return gradesService.getGradesById(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.FOUND)
    public List<GradesResponseDto> getAllGrades(){
        return gradesService.getAllGrades();
    }

    @GetMapping("/student/{studentId}/average")
    @ResponseStatus(HttpStatus.OK)
    public double getAverageGradeByStudentId(@PathVariable Long studentId){
        return gradesService.getAverageGradeByStudentId(studentId);
    }
}
