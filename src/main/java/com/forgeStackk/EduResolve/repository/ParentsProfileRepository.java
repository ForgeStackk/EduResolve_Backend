package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ParentsProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentsProfileRepository extends JpaRepository<ParentsProfile, Long> {
    Optional<ParentsProfile> findByUserId(Long userId);
    List<ParentsProfile> findByClassName(String className);
}
