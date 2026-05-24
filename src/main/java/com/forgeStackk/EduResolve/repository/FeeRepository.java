package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByStatus(Fee.Status status);
    List<Fee> findByStudentId(Long studentId);

    @Query(value = "SELECT * FROM fee WHERE (:status = '' OR status = :status) " +
                   "AND (:search = '' OR LOWER(student_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                   "ORDER BY due_date ASC",
           countQuery = "SELECT COUNT(*) FROM fee WHERE (:status = '' OR status = :status) " +
                        "AND (:search = '' OR LOWER(student_name) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Fee> searchFees(@Param("status") String status,
                         @Param("search") String search,
                         Pageable pageable);
}
