package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Student;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.GradesRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.StudentRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.SubjectRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.GradeService;
import com.codeWithJeff.SampleSpringBootApplication.Util.GradeCalculator;
import com.codeWithJeff.SampleSpringBootApplication.dto.GradesRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.GradesResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor

public class GradesImplementation implements GradeService {


    private final GradesRepository gradesRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;


    @Override
    public GradesResponseDto createGrades(GradesRequestDto gradesRequestDto){
        if(gradesRepository.existsByStudent_StudentIdAndSubject_SubjectId(
                gradesRequestDto.getStudentId(),gradesRequestDto.getSubjectId())){
            throw new ResourceAlreadyExistsException("Student already has a grade for this subject");
        }
        Student student = studentRepository.findById(gradesRequestDto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Subject subject = subjectRepository.findById(gradesRequestDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Grades grades = Grades.builder()
                .student(student)
                .subject(subject)
                .grade(gradesRequestDto.getGrade())
                .build();
        Grades save = gradesRepository.save(grades);
        return gradesResponse(save);
    }

    @Override
    public GradesResponseDto getGradesById(Long id){
       Grades grades = gradesRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
       return gradesResponse(grades);
    }

    @Override
    public List<GradesResponseDto> getAllGrades() {
        return gradesRepository.findAll().stream().map(this::gradesResponse).toList();
    };

    @Override
    public double getAverageGradeByStudentId(Long studentId){
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        List<Grades> gradesList = gradesRepository.findByStudent_StudentId(studentId);

        return GradeCalculator.calculateAverageGrade(gradesList);
    }

    private GradesResponseDto gradesResponse(Grades grades){
        return GradesResponseDto.builder()
                .gradeId(grades.getGradeId())
                .studentName(grades.getStudent().getFirstName()+" "+grades.getStudent().getLastName())
                .subject(grades.getSubject().getSubject())
                .grade(grades.getGrade())
                .build();
    }
}
