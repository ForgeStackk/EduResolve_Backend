package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdOrderByOrderIndexAscIdAsc(Long subjectId);
}
