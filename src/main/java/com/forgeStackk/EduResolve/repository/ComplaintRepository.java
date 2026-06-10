package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByParentIdOrderByCreatedAtDesc(Long parentId);
}
