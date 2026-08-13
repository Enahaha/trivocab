package com.trivocab.ielts.common;

import com.trivocab.ielts.domain.UserAccountRow;
import com.trivocab.ielts.mapper.AuthMapper;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Resolves the personal time zone of a user for date-bucketed features
 * (check-ins, daily stats, streak and completion-date estimation). Falls back
 * to the application default zone when the account has no usable value.
 */
@Component
public class UserTimezoneProvider {
    private final AuthMapper authMapper;
    private final ZoneId fallback;

    public UserTimezoneProvider(AuthMapper authMapper, ZoneId applicationZoneId) {
        this.authMapper = authMapper;
        this.fallback = applicationZoneId;
    }

    public ZoneId zoneOf(long userId) {
        UserAccountRow user = authMapper.findUserById(userId);
        if (user == null || user.getTimeZone() == null || user.getTimeZone().isBlank()) {
            return fallback;
        }
        try {
            return ZoneId.of(user.getTimeZone().trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
