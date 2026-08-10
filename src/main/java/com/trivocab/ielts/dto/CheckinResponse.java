package com.trivocab.ielts.dto;

import java.time.LocalDate;
import java.util.List;

public record CheckinResponse(
        boolean todayCheckedIn,
        int streak,
        int totalCheckins,
        List<LocalDate> dates,
        int year,
        int month
) {
}
