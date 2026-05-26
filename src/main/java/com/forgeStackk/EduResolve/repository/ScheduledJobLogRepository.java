package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ScheduledJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, Long> {
}
