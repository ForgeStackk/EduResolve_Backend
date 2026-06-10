package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.UserLogin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLoginRepository extends JpaRepository<UserLogin, Long> {
    Optional<UserLogin> findFirstByEmail(String email);
    Optional<UserLogin> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserLogin> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM user_login WHERE LOWER(role) = LOWER(:role) " +
                   "AND (:search = '' OR LOWER(first_name || ' ' || last_name) LIKE LOWER('%' || :search || '%') " +
                   "  OR LOWER(email) LIKE LOWER('%' || :search || '%')) " +
                   "AND (:className = '' OR LOWER(class_name) LIKE LOWER('%' || :className || '%')) " +
                   "ORDER BY first_name ASC, last_name ASC",
           countQuery = "SELECT COUNT(*) FROM user_login WHERE LOWER(role) = LOWER(:role) " +
                        "AND (:search = '' OR LOWER(first_name || ' ' || last_name) LIKE LOWER('%' || :search || '%') " +
                        "  OR LOWER(email) LIKE LOWER('%' || :search || '%')) " +
                        "AND (:className = '' OR LOWER(class_name) LIKE LOWER('%' || :className || '%'))",
           nativeQuery = true)
    Page<UserLogin> findByRoleAndSearch(@Param("role") String role,
                                        @Param("search") String search,
                                        @Param("className") String className,
                                        Pageable pageable);
}
