package com.intellias.urlapp.domain.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a URL mapping in the system.
 * This is the core business entity that encapsulates the URL shortening logic.
 */
public class UrlMapping {
    private final String shortCode;
    private final String originalUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private int clickCount;
    private LocalDateTime lastAccessedAt;

    public UrlMapping(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        this.shortCode = validateShortCode(shortCode);
        this.originalUrl = validateOriginalUrl(originalUrl);
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.clickCount = 0;
        this.lastAccessedAt = null;
    }

    public UrlMapping(String shortCode, String originalUrl, LocalDateTime createdAt, 
                     LocalDateTime expiresAt, int clickCount, LocalDateTime lastAccessedAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.lastAccessedAt = lastAccessedAt;
    }

    private String validateShortCode(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }
        if (shortCode.length() > 10) {
            throw new IllegalArgumentException("Short code cannot exceed 10 characters");
        }
        return shortCode.trim();
    }

    private String validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original URL cannot be null or empty");
        }
        String trimmedUrl = originalUrl.trim();
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        return trimmedUrl;
    }

    public void recordAccess() {
        this.clickCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isExpired();
    }

    // Getters
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getClickCount() { return clickCount; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlMapping that = (UrlMapping) o;
        return Objects.equals(shortCode, that.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }

    @Override
    public String toString() {
        return "UrlMapping{" +
                "shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", createdAt=" + createdAt +
                ", clickCount=" + clickCount +
                '}';
    }
}