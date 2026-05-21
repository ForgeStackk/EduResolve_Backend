package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.DoubtMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoubtMessageAttachmentRepository extends JpaRepository<DoubtMessageAttachment, Long> {
    List<DoubtMessageAttachment> findByDoubtMessageId(Long doubtMessageId);
}
