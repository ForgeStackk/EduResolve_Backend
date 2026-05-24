package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByParentUserIdOrderByCreatedAtDesc(Long parentUserId);
    List<LeaveApplication> findByClassNameOrderByCreatedAtDesc(String className);
}
