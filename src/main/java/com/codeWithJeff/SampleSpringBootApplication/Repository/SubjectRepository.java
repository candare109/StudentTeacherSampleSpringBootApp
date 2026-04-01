package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Subject;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubject(String subject);

    boolean existsByTeacher_TeacherId(Long teacherId);

    @Override
    List<Subject> findAll(Sort sort);
}
