package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ComplaintReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintReplyRepository extends JpaRepository<ComplaintReply, Long> {
    List<ComplaintReply> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
