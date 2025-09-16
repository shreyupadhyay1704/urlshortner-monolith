package com.intellias.urlapp.application.usecases;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for retrieving and redirecting to original URLs.
 * This orchestrates the business logic for URL resolution and access tracking.
 */
@Service
@Transactional
public class RetrieveUrlUseCase {
    
    private final UrlMappingRepository urlMappingRepository;
    
    public RetrieveUrlUseCase(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }
    
    /**
     * Retrieves the original URL for a given short code and records the access
     * @param shortCode The short code to resolve
     * @return Response with original URL or error details
     */
    public RetrieveUrlResponse execute(String shortCode) {
        Optional<UrlMapping> mappingOpt = urlMappingRepository.findByShortCode(shortCode);
        
        if (mappingOpt.isEmpty()) {
            return RetrieveUrlResponse.notFound();
        }
        
        UrlMapping mapping = mappingOpt.get();
        
        if (mapping.isExpired()) {
            return RetrieveUrlResponse.expired();
        }
        
        // Record the access
        mapping.recordAccess();
        urlMappingRepository.updateClickStats(mapping);
        
        return RetrieveUrlResponse.success(mapping.getOriginalUrl());
    }
    
    public static class RetrieveUrlResponse {
        private final String originalUrl;
        private final boolean found;
        private final boolean expired;
        private final String message;
        
        private RetrieveUrlResponse(String originalUrl, boolean found, boolean expired, String message) {
            this.originalUrl = originalUrl;
            this.found = found;
            this.expired = expired;
            this.message = message;
        }
        
        public static RetrieveUrlResponse success(String originalUrl) {
            return new RetrieveUrlResponse(originalUrl, true, false, "URL found");
        }
        
        public static RetrieveUrlResponse notFound() {
            return new RetrieveUrlResponse(null, false, false, "Short URL not found");
        }
        
        public static RetrieveUrlResponse expired() {
            return new RetrieveUrlResponse(null, false, true, "Short URL has expired");
        }
        
        public String getOriginalUrl() { return originalUrl; }
        public boolean isFound() { return found; }
        public boolean isExpired() { return expired; }
        public String getMessage() { return message; }
        public boolean isSuccessful() { return found && !expired; }
    }
}