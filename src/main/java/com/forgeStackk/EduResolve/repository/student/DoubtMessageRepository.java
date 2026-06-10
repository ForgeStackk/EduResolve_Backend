package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.DoubtMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoubtMessageRepository extends JpaRepository<DoubtMessage, Long> {
    List<DoubtMessage> findByThreadIdOrderBySentAtAsc(Long threadId);
}
