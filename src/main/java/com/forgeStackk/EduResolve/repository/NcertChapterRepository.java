package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.NcertChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NcertChapterRepository extends JpaRepository<NcertChapter, Long> {

    List<NcertChapter> findByBookIdOrderByOrderIndex(Long bookId);

    Optional<NcertChapter> findByBookIdAndChapterNumber(Long bookId, Integer chapterNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM NcertChapter c WHERE c.bookId = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);

    @Query("SELECT c FROM NcertChapter c WHERE c.bookId = :bookId ORDER BY c.orderIndex")
    List<NcertChapter> findByBookIdOrderByOrderIndexAsc(@Param("bookId") Long bookId);
}
