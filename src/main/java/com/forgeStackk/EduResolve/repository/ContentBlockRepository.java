package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {

    List<ContentBlock> findByChapterIdOrderByOrderIndex(Long chapterId);

    List<ContentBlock> findByChapterIdAndBlockTypeOrderByOrderIndex(Long chapterId, ContentBlock.BlockType blockType);

    @Modifying
    @Transactional
    @Query("DELETE FROM ContentBlock c WHERE c.chapterId = :chapterId")
    void deleteByChapterId(@Param("chapterId") Long chapterId);

    @Query("SELECT c FROM ContentBlock c WHERE LOWER(c.contentText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<ContentBlock> findByContentTextContainingIgnoreCase(@Param("searchText") String searchText);

    @Query("SELECT c FROM ContentBlock c WHERE c.chapterId = :chapterId AND c.pageNumber = :pageNumber ORDER BY c.orderIndex")
    List<ContentBlock> findByChapterIdAndPageNumberOrderByOrderIndex(@Param("chapterId") Long chapterId, @Param("pageNumber") Integer pageNumber);

    List<ContentBlock> findByChapterIdOrderByOrderIndexAsc(Long chapterId);

    List<ContentBlock> findByChapterIdAndBlockTypeOrderByOrderIndexAsc(Long chapterId, ContentBlock.BlockType blockType);
}
