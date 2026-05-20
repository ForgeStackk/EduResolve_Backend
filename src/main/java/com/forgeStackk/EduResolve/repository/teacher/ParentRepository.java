package com.forgeStackk.EduResolve.repository.teacher;

import com.forgeStackk.EduResolve.entity.teacher.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    Optional<Parent> findByEmail(String email);
    Optional<Parent> findByPhone(String phone);
    Optional<Parent> findByUserId(Long userId);
}
