package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.ClassAssignment;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Student;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Teacher;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.ClassAssignmentRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.StudentRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.SubjectRepository;
import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.ClassAssignmentService;
import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentRequest;
import com.codeWithJeff.SampleSpringBootApplication.dto.ClassAssignmentResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassAssignmentServiceImplementation implements ClassAssignmentService {

    private final ClassAssignmentRepository classAssignmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public ClassAssignmentResponse createAssignment(ClassAssignmentRequest requestDto) {
        // Check for duplicate assignment
        if (classAssignmentRepository.existsByStudent_StudentIdAndSubject_SubjectIdAndTeacher_TeacherId(
                requestDto.getStudentId(), requestDto.getSubjectId(), requestDto.getTeacherId())) {
            throw new ResourceAlreadyExistsException("This class assignment already exists");
        }

        // Look up the entities by their IDs
        Student student = studentRepository.findById(requestDto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Subject subject = subjectRepository.findById(requestDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Teacher teacher = teacherRepository.findById(requestDto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        // Build and save the assignment
        ClassAssignment assignment = ClassAssignment.builder()
                .student(student)
                .subject(subject)
                .teacher(teacher)
                .build();
        ClassAssignment saved = classAssignmentRepository.save(assignment);

        return toResponse(saved);
    }

    @Override
    public List<ClassAssignmentResponse> getAllAssignments() {
        return classAssignmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public ClassAssignmentResponse getAssignmentById(Long id) {
        ClassAssignment assignment = classAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class assignment not found"));
        return toResponse(assignment);
    }

    @Override
    public ClassAssignmentResponse updateAssignmentById(Long id, ClassAssignmentRequest requestDto) {
        ClassAssignment assignment = classAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class assignment not found"));

        Student student = studentRepository.findById(requestDto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Subject subject = subjectRepository.findById(requestDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Teacher teacher = teacherRepository.findById(requestDto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        assignment.setStudent(student);
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);

        return toResponse(classAssignmentRepository.save(assignment));
    }

    @Override
    public void deleteAssignmentById(Long id) {
        ClassAssignment assignment = classAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class assignment not found"));
        classAssignmentRepository.delete(assignment);
    }

    private ClassAssignmentResponse toResponse(ClassAssignment assignment) {
        Student student = assignment.getStudent();
        Subject subject = assignment.getSubject();
        Teacher teacher = assignment.getTeacher();

        return ClassAssignmentResponse.builder()
                .classAssignmentId(assignment.getClassAssignmentId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .subjectName(subject.getSubject())
                .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
                .build();
    }
}

