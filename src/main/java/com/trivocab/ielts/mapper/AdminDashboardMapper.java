package com.trivocab.ielts.mapper;

import com.trivocab.ielts.domain.AdminDashboardStatsRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface AdminDashboardMapper {
    AdminDashboardStatsRow findStats(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );
}
