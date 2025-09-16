package com.intellias.urlapp.infrastructure.repositories;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import com.intellias.urlapp.infrastructure.mappers.UrlMappingMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the domain repository interface using JPA.
 * This bridges the domain layer with the infrastructure layer.
 */
@Repository
@Transactional
public class UrlMappingRepositoryImpl implements UrlMappingRepository {
    
    private final JpaUrlMappingRepository jpaRepository;
    private final UrlMappingMapper mapper;
    
    public UrlMappingRepositoryImpl(JpaUrlMappingRepository jpaRepository, UrlMappingMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public UrlMapping save(UrlMapping urlMapping) {
        // Check if this is an update to an existing entity
        Optional<UrlMappingEntity> existingEntity = jpaRepository.findByShortCode(urlMapping.getShortCode());
        
        UrlMappingEntity entityToSave;
        if (existingEntity.isPresent()) {
            // Update existing entity to maintain JPA managed state
            entityToSave = existingEntity.get();
            entityToSave.setOriginalUrl(urlMapping.getOriginalUrl());
            entityToSave.setExpiresAt(urlMapping.getExpiresAt());
            entityToSave.setClickCount(urlMapping.getClickCount());
            entityToSave.setLastAccessedAt(urlMapping.getLastAccessedAt());
        } else {
            // Create new entity
            entityToSave = mapper.toEntity(urlMapping);
        }
        
        UrlMappingEntity savedEntity = jpaRepository.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<UrlMapping> findByShortCode(String shortCode) {
        return jpaRepository.findByShortCode(shortCode)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<UrlMapping> findByOriginalUrl(String originalUrl) {
        return jpaRepository.findByOriginalUrl(originalUrl)
                .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByShortCode(String shortCode) {
        return jpaRepository.existsByShortCode(shortCode);
    }
    
    @Override
    public List<UrlMapping> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteByShortCode(String shortCode) {
        jpaRepository.deleteByShortCode(shortCode);
    }
    
    @Override
    public UrlMapping updateClickStats(UrlMapping urlMapping) {
        // Use atomic DB-level increment to avoid JPA persistence-context staleness
        if (jpaRepository.existsByShortCode(urlMapping.getShortCode())) {
            // Perform atomic increment directly in database
            jpaRepository.incrementClickCount(urlMapping.getShortCode(), urlMapping.getLastAccessedAt());
            
            // Return fresh entity from database to ensure we have the actual state
            return jpaRepository.findByShortCode(urlMapping.getShortCode())
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException("Entity should exist after atomic update"));
        } else {
            // Fallback to regular save if entity doesn't exist
            return save(urlMapping);
        }
    }
}