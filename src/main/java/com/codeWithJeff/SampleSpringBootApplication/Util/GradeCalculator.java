package com.codeWithJeff.SampleSpringBootApplication.Util;
import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GradeCalculator {
    public static double calculateAverageGrade(List<Grades> gradesList){
        if(gradesList == null || gradesList.isEmpty()){
            return 0.0;
        }
        return gradesList.stream()
                .mapToDouble(Grades::getGrade)
                .average()
                .orElse(0.0);
    }
}
