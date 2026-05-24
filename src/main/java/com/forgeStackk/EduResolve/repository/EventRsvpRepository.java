package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRsvpRepository extends JpaRepository<EventRsvp, Long> {
    Optional<EventRsvp> findByEventIdAndUserId(Long eventId, Long userId);
    List<EventRsvp>     findByUserId(Long userId);
    long                countByEventId(Long eventId);
}
