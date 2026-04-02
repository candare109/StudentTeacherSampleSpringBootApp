package com.codeWithJeff.SampleSpringBootApplication.Implementation;


import com.codeWithJeff.SampleSpringBootApplication.Entity.Student;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Teacher;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.StudentRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.SubjectRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.SubjectService;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectDto;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SubjectServiceImplementation implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public SubjectDto createSubject(SubjectDto requestSubjectDto){
        // 1. Check if THIS student is already enrolled in THIS subject
        if(subjectRepository.existsByStudent_StudentIdAndSubject(
                requestSubjectDto.getStudentId(), requestSubjectDto.getSubject())){
            throw new ResourceAlreadyExistsException("Student is already enrolled in this subject");
        }

        // 2. If subject already exists, verify the teacher matches (one teacher per subject)
        subjectRepository.findFirstBySubject(requestSubjectDto.getSubject()).ifPresent(existing -> {
            if (!existing.getTeacher().getTeacherId().equals(requestSubjectDto.getTeacherId())) {
                throw new ResourceAlreadyExistsException(
                        "Subject already has a different teacher assigned (teacher_id: "
                                + existing.getTeacher().getTeacherId() + ")");
            }
        });

        // 3. Look up Teacher and Student
        Teacher teacher = teacherRepository.findById(requestSubjectDto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        Student student = studentRepository.findById(requestSubjectDto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

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


    @Override
    public List<SubjectDto> getAllSubjects(){
        return subjectRepository.findAll().stream().map(this::subjectGetAllResponse).toList();
    }

    private SubjectDto subjectGetAllResponse(Subject subject){
        Student student = subject.getStudent();
        Teacher teacher = subject.getTeacher();
        return  SubjectDto.builder()
                .subjectId(subject.getSubjectId())
                .studentId(student.getStudentId())
                .teacherId(teacher.getTeacherId())
                .subject(subject.getSubject())
                .studentName(student.getFirstName()+" "+student.getLastName())
                .teacherName(teacher.getFirstName()+" "+teacher.getLastName())
                .build();
    }

}
