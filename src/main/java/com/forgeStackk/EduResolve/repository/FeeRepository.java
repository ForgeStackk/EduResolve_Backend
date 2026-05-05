package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByStatus(Fee.Status status);
    List<Fee> findByStudentId(Long studentId);
}
