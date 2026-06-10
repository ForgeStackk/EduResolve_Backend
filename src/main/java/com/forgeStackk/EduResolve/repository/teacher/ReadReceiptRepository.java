package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, UUID> {
    List<ReadReceipt> findByMessageId(UUID messageId);
    Optional<ReadReceipt> findByMessageIdAndRecipientId(UUID messageId, UUID recipientId);
    long countByMessageId(UUID messageId);
}
