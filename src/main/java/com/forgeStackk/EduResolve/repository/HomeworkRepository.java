package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    List<Homework> findAllByOrderByCreatedAtDesc();
    List<Homework> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<Homework> findByClassNameOrderByCreatedAtDesc(String className);
}
