package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.AtRiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtRiskSnapshotRepository extends JpaRepository<AtRiskSnapshot, Long> {
    Optional<AtRiskSnapshot> findTopByStudentSeqIdOrderBySnapshotDateDesc(Long studentSeqId);
    Optional<AtRiskSnapshot> findByStudentSeqIdAndSnapshotDate(Long studentSeqId, LocalDate date);

    @Query("SELECT s FROM AtRiskSnapshot s WHERE s.studentSeqId IN :studentSeqIds " +
           "AND s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM AtRiskSnapshot s2 WHERE s2.studentSeqId = s.studentSeqId)")
    List<AtRiskSnapshot> findLatestForStudents(@Param("studentSeqIds") List<Long> studentSeqIds);
}
