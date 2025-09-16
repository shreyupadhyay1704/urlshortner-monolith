package com.intellias.urlapp.domain.repositories;

import com.intellias.urlapp.domain.entities.UrlMapping;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UrlMapping domain entity.
 * This is part of the domain layer and defines the contract for data access
 * without depending on specific infrastructure implementations.
 */
public interface UrlMappingRepository {
    
    /**
     * Saves a URL mapping
     * @param urlMapping The URL mapping to save
     * @return The saved URL mapping
     */
    UrlMapping save(UrlMapping urlMapping);
    
    /**
     * Finds a URL mapping by its short code
     * @param shortCode The short code to search for
     * @return Optional containing the URL mapping if found
     */
    Optional<UrlMapping> findByShortCode(String shortCode);
    
    /**
     * Finds a URL mapping by its original URL
     * @param originalUrl The original URL to search for
     * @return Optional containing the URL mapping if found
     */
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
    
    /**
     * Checks if a short code already exists
     * @param shortCode The short code to check
     * @return true if exists, false otherwise
     */
    boolean existsByShortCode(String shortCode);
    
    /**
     * Finds all URL mappings (with pagination support)
     * @return List of all URL mappings
     */
    List<UrlMapping> findAll();
    
    /**
     * Deletes a URL mapping by short code
     * @param shortCode The short code of the mapping to delete
     */
    void deleteByShortCode(String shortCode);
    
    /**
     * Updates the click count and last accessed time for a URL mapping
     * @param urlMapping The URL mapping to update
     * @return The updated URL mapping
     */
    UrlMapping updateClickStats(UrlMapping urlMapping);
}