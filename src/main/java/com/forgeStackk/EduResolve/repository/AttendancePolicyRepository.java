package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, Long> {
    Optional<AttendancePolicy> findBySchoolNameIgnoreCase(String schoolName);
}
