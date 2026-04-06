package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.SubjectRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.SubjectService;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.SubjectResponseDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SubjectServiceImplementation implements SubjectService {
    private final SubjectRepository subjectRepository;

    @Override
    public SubjectResponseDto createSubject(SubjectRequestDto requestDto) {
        if (subjectRepository.existsBySubject(requestDto.getSubject())) {
            throw new ResourceAlreadyExistsException("Subject already exists");
        }

        Subject subject = Subject.builder()
                .subject(requestDto.getSubject())
                .build();

        return toResponse(subjectRepository.save(subject));
    }

    @Override
    public List<SubjectResponseDto> getAllSubjects() {
        return subjectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public SubjectResponseDto getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return toResponse(subject);
    }

    @Override
    public SubjectResponseDto updateSubjectById(Long id, SubjectRequestDto requestDto) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subject.setSubject(requestDto.getSubject());

        return toResponse(subjectRepository.save(subject));
    }

    @Override
    public void deleteSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        subjectRepository.delete(subject);
    }

    private SubjectResponseDto toResponse(Subject subject) {
        return SubjectResponseDto.builder()
                .subjectId(subject.getSubjectId())
                .subject(subject.getSubject())
                .build();
    }

}
