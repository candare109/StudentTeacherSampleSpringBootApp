package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.ClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    // Prevent duplicate: same student + subject + teacher
    boolean existsByStudent_StudentIdAndSubject_SubjectIdAndTeacher_TeacherId(
            Long studentId, Long subjectId, Long teacherId);

}

