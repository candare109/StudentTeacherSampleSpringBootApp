package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Teacher;
import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.TeacherService;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor

public class TeacherServiceImplementation implements TeacherService {
    private final TeacherRepository teacherRepository;

    @Override
    public TeacherResponseDto createTeacher(TeacherRequestDto requestDto) {
        if (!teacherRepository.findByCourse(requestDto.getCourse()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already exists");
        }

        Teacher teacher = Teacher.builder()
                .firstName(requestDto.getFirstName())
                .lastName(requestDto.getLastName())
                .course(requestDto.getCourse())
                .build();

        return toResponseTeacher(teacherRepository.save(teacher));
    }

    @Override
    public List<TeacherResponseDto> getAllTeachers(){
        return teacherRepository.findAll().stream().map(this::toResponseTeacher).toList();
    }

    @Override
    public TeacherResponseDto getTeacherById(Long id){
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teachers not found"));
        return toResponseTeacher(teacher);
    }

    @Override
    public void deleteTeacherById(Long id){
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        teacherRepository.delete(teacher);
    }

    private TeacherResponseDto toResponseTeacher(Teacher teacher){
        return TeacherResponseDto.builder()
                .id(teacher.getId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .course(teacher.getCourse())
                .build();
    }



}
