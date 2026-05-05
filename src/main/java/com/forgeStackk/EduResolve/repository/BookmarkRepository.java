package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<Bookmark> findByStudentIdAndTargetTypeAndTargetId(
        Long studentId, Bookmark.TargetType targetType, Long targetId);

    void deleteByStudentIdAndTargetTypeAndTargetId(
        Long studentId, Bookmark.TargetType targetType, Long targetId);
}
