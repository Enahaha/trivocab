package com.trivocab.ielts.dto;

import java.time.LocalDate;

public record DailyStudyStat(
        LocalDate date,
        int learnedWords,
        int reviewedWords,
        long studyMinutes
) {
}
