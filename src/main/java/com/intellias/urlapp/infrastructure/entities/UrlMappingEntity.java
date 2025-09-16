package com.intellias.urlapp.infrastructure.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for URL mapping persistence.
 * This is the infrastructure layer's representation of the domain entity.
 */
@Entity
@Table(name = "url_mappings")
public class UrlMappingEntity {
    
    @Id
    @Column(name = "short_code", length = 10)
    private String shortCode;
    
    @Column(name = "original_url", length = 2048, nullable = false)
    private String originalUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "click_count", nullable = false)
    private int clickCount = 0;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    // Default constructor for JPA
    public UrlMappingEntity() {
    }
    
    public UrlMappingEntity(String shortCode, String originalUrl, LocalDateTime createdAt,
                           LocalDateTime expiresAt, int clickCount, LocalDateTime lastAccessedAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.lastAccessedAt = lastAccessedAt;
    }
    
    // Getters and setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
}