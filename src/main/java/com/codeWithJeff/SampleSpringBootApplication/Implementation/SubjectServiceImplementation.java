package com.codeWithJeff.SampleSpringBootApplication.Implementation;


import com.codeWithJeff.SampleSpringBootApplication.Entity.Student;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Teacher;
import com.codeWithJeff.SampleSpringBootApplication.Repository.StudentRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.SubjectRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.SubjectService;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectDto;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;



@Service
@RequiredArgsConstructor
public class SubjectServiceImplementation implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public SubjectDto createSubject(SubjectDto requestSubjectDto){
        //Creating Validation logics
        if(subjectRepository.existsBySubject(requestSubjectDto.getSubject())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists");
        }
        if(subjectRepository.existsByTeacher_TeacherId(requestSubjectDto.getTeacherId())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Teacher already has a subject assigned");
        }
        Teacher teacher = teacherRepository.findById(requestSubjectDto.getTeacherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        Student student = studentRepository.findById(requestSubjectDto.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        //Building Subject Response
        Subject subject = Subject.builder()
                .student(student)
                .teacher(teacher)
                .subject(requestSubjectDto.getSubject())
                .build();
        Subject saved = subjectRepository.save(subject);

        //return response
        return SubjectDto.builder()
                .subjectId(saved.getSubjectId())
                .studentId(student.getStudentId())
                .teacherId(teacher.getTeacherId())
                .subject(saved.getSubject())
                .studentName(student.getFirstName()+" "+student.getLastName())
                .teacherName(teacher.getFirstName()+" "+teacher.getLastName())
                .build();
    }

}
