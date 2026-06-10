package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUserId(Long userId);
    List<TeacherProfile> findBySubject(String subject);
    List<TeacherProfile> findByClassName(String className);
}
