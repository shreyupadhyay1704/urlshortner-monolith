package com.intellias.urlapp.presentation.dto;

/**
 * Data Transfer Object for URL shortening responses.
 * This represents the presentation layer's output format.
 */
public class ShortenUrlResponseDto {
    
    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private boolean isCustom;
    private String expiresAt;
    private String message;
    
    public ShortenUrlResponseDto() {
    }
    
    public ShortenUrlResponseDto(String shortCode, String originalUrl, String shortUrl,
                               boolean isCustom, String expiresAt, String message) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.isCustom = isCustom;
        this.expiresAt = expiresAt;
        this.message = message;
    }
    
    // Getters and setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    
    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }
    
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}