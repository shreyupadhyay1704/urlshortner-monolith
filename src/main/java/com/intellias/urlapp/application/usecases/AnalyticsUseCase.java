package com.intellias.urlapp.application.usecases;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Use case for URL analytics and statistics.
 * This orchestrates the business logic for retrieving URL usage analytics.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsUseCase {
    
    private final UrlMappingRepository urlMappingRepository;
    
    public AnalyticsUseCase(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }
    
    /**
     * Gets analytics for a specific short URL
     * @param shortCode The short code to get analytics for
     * @return Analytics response with statistics
     */
    public UrlAnalyticsResponse getUrlAnalytics(String shortCode) {
        Optional<UrlMapping> mappingOpt = urlMappingRepository.findByShortCode(shortCode);
        
        if (mappingOpt.isEmpty()) {
            return UrlAnalyticsResponse.notFound();
        }
        
        UrlMapping mapping = mappingOpt.get();
        return UrlAnalyticsResponse.fromUrlMapping(mapping);
    }
    
    /**
     * Gets overall system analytics
     * @return System analytics with aggregate statistics
     */
    public SystemAnalyticsResponse getSystemAnalytics() {
        List<UrlMapping> allMappings = urlMappingRepository.findAll();
        
        long totalUrls = allMappings.size();
        long totalClicks = allMappings.stream().mapToLong(UrlMapping::getClickCount).sum();
        long activeUrls = allMappings.stream().filter(UrlMapping::isActive).count();
        long expiredUrls = totalUrls - activeUrls;
        
        List<UrlAnalyticsResponse> topUrls = allMappings.stream()
            .sorted((a, b) -> Integer.compare(b.getClickCount(), a.getClickCount()))
            .limit(10)
            .map(UrlAnalyticsResponse::fromUrlMapping)
            .collect(Collectors.toList());
        
        return new SystemAnalyticsResponse(totalUrls, totalClicks, activeUrls, expiredUrls, topUrls);
    }
    
    public static class UrlAnalyticsResponse {
        private final String shortCode;
        private final String originalUrl;
        private final int clickCount;
        private final String createdAt;
        private final String lastAccessedAt;
        private final boolean isActive;
        private final String expiresAt;
        private final boolean found;
        
        public UrlAnalyticsResponse(String shortCode, String originalUrl, int clickCount, 
                                  String createdAt, String lastAccessedAt, boolean isActive,
                                  String expiresAt, boolean found) {
            this.shortCode = shortCode;
            this.originalUrl = originalUrl;
            this.clickCount = clickCount;
            this.createdAt = createdAt;
            this.lastAccessedAt = lastAccessedAt;
            this.isActive = isActive;
            this.expiresAt = expiresAt;
            this.found = found;
        }
        
        public static UrlAnalyticsResponse fromUrlMapping(UrlMapping mapping) {
            return new UrlAnalyticsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt() != null ? mapping.getCreatedAt().toString() : null,
                mapping.getLastAccessedAt() != null ? mapping.getLastAccessedAt().toString() : null,
                mapping.isActive(),
                mapping.getExpiresAt() != null ? mapping.getExpiresAt().toString() : null,
                true
            );
        }
        
        public static UrlAnalyticsResponse notFound() {
            return new UrlAnalyticsResponse(null, null, 0, null, null, false, null, false);
        }
        
        // Getters
        public String getShortCode() { return shortCode; }
        public String getOriginalUrl() { return originalUrl; }
        public int getClickCount() { return clickCount; }
        public String getCreatedAt() { return createdAt; }
        public String getLastAccessedAt() { return lastAccessedAt; }
        public boolean isActive() { return isActive; }
        public String getExpiresAt() { return expiresAt; }
        public boolean isFound() { return found; }
    }
    
    public static class SystemAnalyticsResponse {
        private final long totalUrls;
        private final long totalClicks;
        private final long activeUrls;
        private final long expiredUrls;
        private final List<UrlAnalyticsResponse> topUrls;
        
        public SystemAnalyticsResponse(long totalUrls, long totalClicks, long activeUrls,
                                     long expiredUrls, List<UrlAnalyticsResponse> topUrls) {
            this.totalUrls = totalUrls;
            this.totalClicks = totalClicks;
            this.activeUrls = activeUrls;
            this.expiredUrls = expiredUrls;
            this.topUrls = topUrls;
        }
        
        // Getters
        public long getTotalUrls() { return totalUrls; }
        public long getTotalClicks() { return totalClicks; }
        public long getActiveUrls() { return activeUrls; }
        public long getExpiredUrls() { return expiredUrls; }
        public List<UrlAnalyticsResponse> getTopUrls() { return topUrls; }
    }
}