package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.NcertBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NcertBookRepository extends JpaRepository<NcertBook, Long> {

    List<NcertBook> findByClassGrade(String classGrade);

    List<NcertBook> findByClassGradeAndSubject(String classGrade, String subject);

    Optional<NcertBook> findByClassGradeAndSubjectAndTitle(String classGrade, String subject, String title);

    boolean existsByClassGradeAndSubjectAndTitle(String classGrade, String subject, String title);

    // GitHub-related methods
    List<NcertBook> findBySubject(String subject);

    Optional<NcertBook> findByTitle(String title);

    @Query("SELECT b FROM NcertBook b WHERE LOWER(b.classGrade) = LOWER(:classGrade)")
    List<NcertBook> findByClassGradeIgnoreCase(@Param("classGrade") String classGrade);

    @Query("SELECT b FROM NcertBook b WHERE LOWER(b.subject) = LOWER(:subject)")
    List<NcertBook> findBySubjectIgnoreCase(@Param("subject") String subject);

    @Query("SELECT b FROM NcertBook b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<NcertBook> findByTitleContainingIgnoreCase(@Param("title") String title);

    List<NcertBook> findByGithubRepo(String githubRepo);

    Optional<NcertBook> findByGithubPath(String githubPath);

    @Query("SELECT COUNT(b) FROM NcertBook b WHERE b.classGrade = :classGrade AND b.subject = :subject")
    long countByClassGradeAndSubject(@Param("classGrade") String classGrade, @Param("subject") String subject);

    @Query("SELECT DISTINCT b.classGrade FROM NcertBook b ORDER BY b.classGrade")
    List<String> findDistinctClassGrades();

    @Query("SELECT DISTINCT b.subject FROM NcertBook b WHERE b.classGrade = :classGrade ORDER BY b.subject")
    List<String> findDistinctSubjectsByClassGrade(@Param("classGrade") String classGrade);
}
