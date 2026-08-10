package com.trivocab.ielts.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

public record DailyGoalRequest(@NotNull @Min(10) @Max(100) Integer dailyGoal) {

    @AssertTrue(message = "dailyGoal 必须以 10 为步长")
    public boolean isValidStep() {
        return dailyGoal == null || dailyGoal % 10 == 0;
    }
}
