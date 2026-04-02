package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubject(String subject);

    boolean existsByTeacher_TeacherId(Long teacherId);

    // Check if THIS student is already enrolled in THIS subject
    boolean existsByStudent_StudentIdAndSubject(Long studentId, String subject);

    // Find an existing subject row to check which teacher is assigned
    Optional<Subject> findFirstBySubject(String subject);

    @Override
    List<Subject> findAll(Sort sort);
}
