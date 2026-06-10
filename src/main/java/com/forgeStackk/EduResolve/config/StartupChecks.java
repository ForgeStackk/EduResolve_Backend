package com.forgeStackk.EduResolve.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
@Slf4j
public class StartupChecks {

    @PostConstruct
    void checkTimezone() {
        String tz = TimeZone.getDefault().getID();
        if (!"UTC".equals(tz) && !"Etc/UTC".equals(tz) && !"GMT".equals(tz)) {
            log.warn(
                "JVM default timezone is '{}', not UTC. " +
                "Attendance dates and audit timestamps may drift against the database (which stores TIMESTAMPTZ in UTC). " +
                "Set -Duser.timezone=UTC or the environment variable TZ=UTC to fix this.",
                tz);
        }
    }
}
