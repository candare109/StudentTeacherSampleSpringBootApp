package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Teacher;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.TeacherRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.TeacherService;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.TeacherResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor

public class TeacherServiceImplementation implements TeacherService {
    private final TeacherRepository teacherRepository;

    @Override
    public TeacherResponseDto createTeacher(TeacherRequestDto requestDto) {
        Teacher teacher = Teacher.builder()
                .firstName(requestDto.getFirstName())
                .lastName(requestDto.getLastName())
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
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        return toResponseTeacher(teacher);
    }

    @Override
    public void deleteTeacherById(Long id){
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacherRepository.delete(teacher);
    }

    @Override
    public TeacherResponseDto updateTeacherById(Long id, TeacherRequestDto teacherRequestDto){
       
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        teacher.setFirstName(teacherRequestDto.getFirstName());
        teacher.setLastName(teacherRequestDto.getLastName());

        return toResponseTeacher(teacherRepository.save(teacher));
    }
    private TeacherResponseDto toResponseTeacher(Teacher teacher){
        return TeacherResponseDto.builder()
                .id(teacher.getTeacherId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .build();
    }



}
