package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.DoubtCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoubtCacheRepository extends JpaRepository<DoubtCache, Long> {
    Optional<DoubtCache> findByQueryHash(String queryHash);
}
