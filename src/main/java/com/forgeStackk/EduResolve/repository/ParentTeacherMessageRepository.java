package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.ParentTeacherMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentTeacherMessageRepository extends JpaRepository<ParentTeacherMessage, Long> {
    List<ParentTeacherMessage> findByParentUserIdOrderByCreatedAtAsc(Long parentUserId);
    List<ParentTeacherMessage> findByClassNameOrderByCreatedAtAsc(String className);
}
