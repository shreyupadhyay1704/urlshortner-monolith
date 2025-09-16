package com.intellias.urlapp.infrastructure.repositories;

import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for URL mapping entities.
 * This provides the basic CRUD operations for the infrastructure layer.
 */
@Repository
public interface JpaUrlMappingRepository extends JpaRepository<UrlMappingEntity, String> {
    
    Optional<UrlMappingEntity> findByShortCode(String shortCode);
    
    Optional<UrlMappingEntity> findByOriginalUrl(String originalUrl);
    
    boolean existsByShortCode(String shortCode);
    
    void deleteByShortCode(String shortCode);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UrlMappingEntity u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :timestamp WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("timestamp") LocalDateTime timestamp);
}