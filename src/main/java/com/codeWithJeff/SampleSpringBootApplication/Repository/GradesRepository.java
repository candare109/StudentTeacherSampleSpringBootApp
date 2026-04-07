package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradesRepository extends JpaRepository<Grades, Long> {
   boolean existsByStudent_StudentIdAndSubject_SubjectId(Long studentId, Long subjectId);

   List<Grades> findByStudent_StudentId(Long studentId);
}
