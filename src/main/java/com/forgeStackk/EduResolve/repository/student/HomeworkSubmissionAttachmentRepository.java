package com.forgeStackk.EduResolve.repository.student;

import com.forgeStackk.EduResolve.entity.student.HomeworkSubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkSubmissionAttachmentRepository extends JpaRepository<HomeworkSubmissionAttachment, Long> {
    List<HomeworkSubmissionAttachment> findBySubmissionId(Long submissionId);
}
