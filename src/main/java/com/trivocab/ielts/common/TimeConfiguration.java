package com.trivocab.ielts.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId applicationZoneId(@Value("${app.time-zone:Asia/Seoul}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}
