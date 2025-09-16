package com.intellias.urlapp.domain.services;

import com.intellias.urlapp.domain.entities.UrlMapping;

/**
 * Domain service interface for URL shortening business logic.
 * This defines the core business operations independent of infrastructure concerns.
 */
public interface UrlShorteningService {
    
    /**
     * Generates a unique short code for the given URL
     * @param originalUrl The original URL to be shortened
     * @return A unique short code
     */
    String generateShortCode(String originalUrl);
    
    /**
     * Validates if a short code is available for use
     * @param shortCode The short code to validate
     * @return true if available, false otherwise
     */
    boolean isShortCodeAvailable(String shortCode);
    
    /**
     * Creates a shortened URL mapping with business validation
     * @param originalUrl The original URL
     * @param customShortCode Optional custom short code
     * @param expirationDays Number of days until expiration (null for no expiration)
     * @return UrlMapping entity
     */
    UrlMapping createShortenedUrl(String originalUrl, String customShortCode, Integer expirationDays);
}