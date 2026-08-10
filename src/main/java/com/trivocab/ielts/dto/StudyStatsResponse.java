package com.trivocab.ielts.dto;

import java.util.List;

public record StudyStatsResponse(
        List<DailyStudyStat> days,
        StudySummary summary
) {
}
