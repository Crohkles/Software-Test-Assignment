// 测试ExerciseType解析
package com.fitnessai.test;

import com.fitnessai.model.ExerciseType;

public class ExerciseTypeTest {
    public static void main(String[] args) {
        String[] testValues = {"squat", "pushup", "plank", "jumping_jack", "unknown"};

        for (String value : testValues) {
            ExerciseType type = ExerciseType.fromId(value);
            System.out.printf("fromId(\"%s\") = %s (id=%s)%n",
                            value, type.name(), type.getId());
        }
    }
}