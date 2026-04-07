package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.ClassAssignment;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    // Prevent duplicate: same student + subject + teacher
    boolean existsByStudent_StudentIdAndSubject_SubjectIdAndTeacher_TeacherId(
            Long studentId, Long subjectId, Long teacherId);



}

