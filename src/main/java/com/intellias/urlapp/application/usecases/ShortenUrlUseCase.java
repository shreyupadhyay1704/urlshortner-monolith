package com.intellias.urlapp.application.usecases;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import com.intellias.urlapp.domain.services.UrlShorteningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for shortening URLs.
 * This orchestrates the business logic for creating shortened URLs.
 */
@Service
@Transactional
public class ShortenUrlUseCase {
    
    private final UrlShorteningService urlShorteningService;
    private final UrlMappingRepository urlMappingRepository;
    
    public ShortenUrlUseCase(UrlShorteningService urlShorteningService, 
                            UrlMappingRepository urlMappingRepository) {
        this.urlShorteningService = urlShorteningService;
        this.urlMappingRepository = urlMappingRepository;
    }
    
    /**
     * Creates a shortened URL with optional custom short code and expiration
     * @param request The request containing URL and optional parameters
     * @return The response with shortened URL details
     */
    public ShortenUrlResponse execute(ShortenUrlRequest request) {
        // Check if URL already exists and is active
        var existingMapping = urlMappingRepository.findByOriginalUrl(request.getOriginalUrl());
        if (existingMapping.isPresent() && existingMapping.get().isActive()) {
            return ShortenUrlResponse.fromUrlMapping(existingMapping.get());
        }
        
        // Create new shortened URL
        UrlMapping urlMapping = urlShorteningService.createShortenedUrl(
            request.getOriginalUrl(),
            request.getCustomShortCode(),
            request.getExpirationDays()
        );
        
        // Save the mapping
        UrlMapping savedMapping = urlMappingRepository.save(urlMapping);
        
        return ShortenUrlResponse.fromUrlMapping(savedMapping);
    }
    
    public static class ShortenUrlRequest {
        private final String originalUrl;
        private final String customShortCode;
        private final Integer expirationDays;
        
        public ShortenUrlRequest(String originalUrl, String customShortCode, Integer expirationDays) {
            this.originalUrl = originalUrl;
            this.customShortCode = customShortCode;
            this.expirationDays = expirationDays;
        }
        
        public String getOriginalUrl() { return originalUrl; }
        public String getCustomShortCode() { return customShortCode; }
        public Integer getExpirationDays() { return expirationDays; }
    }
    
    public static class ShortenUrlResponse {
        private final String shortCode;
        private final String originalUrl;
        private final String shortUrl;
        private final boolean isCustom;
        private final String expiresAt;
        
        public ShortenUrlResponse(String shortCode, String originalUrl, String shortUrl, 
                                boolean isCustom, String expiresAt) {
            this.shortCode = shortCode;
            this.originalUrl = originalUrl;
            this.shortUrl = shortUrl;
            this.isCustom = isCustom;
            this.expiresAt = expiresAt;
        }
        
        public static ShortenUrlResponse fromUrlMapping(UrlMapping mapping) {
            return new ShortenUrlResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                null, // Short URL will be constructed in presentation layer
                false, // We'd need to track this if it matters
                mapping.getExpiresAt() != null ? mapping.getExpiresAt().toString() : null
            );
        }
        
        public String getShortCode() { return shortCode; }
        public String getOriginalUrl() { return originalUrl; }
        public String getShortUrl() { return shortUrl; }
        public boolean isCustom() { return isCustom; }
        public String getExpiresAt() { return expiresAt; }
    }
}