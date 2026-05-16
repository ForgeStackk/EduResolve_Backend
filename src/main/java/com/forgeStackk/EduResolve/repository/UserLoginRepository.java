package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLoginRepository extends JpaRepository<UserLogin, Long> {
    Optional<UserLogin> findFirstByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserLogin> findByUsername(String username);
    boolean existsByUsername(String username);
}
