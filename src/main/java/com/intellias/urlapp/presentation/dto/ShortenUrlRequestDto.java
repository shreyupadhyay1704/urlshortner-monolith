package com.intellias.urlapp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object for URL shortening requests.
 * This represents the presentation layer's input format.
 */
public class ShortenUrlRequestDto {
    
    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String originalUrl;
    
    private String customShortCode;
    
    @Positive(message = "Expiration days must be positive")
    private Integer expirationDays;
    
    // Default constructor
    public ShortenUrlRequestDto() {
    }
    
    public ShortenUrlRequestDto(String originalUrl, String customShortCode, Integer expirationDays) {
        this.originalUrl = originalUrl;
        this.customShortCode = customShortCode;
        this.expirationDays = expirationDays;
    }
    
    // Getters and setters
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    
    public String getCustomShortCode() { return customShortCode; }
    public void setCustomShortCode(String customShortCode) { this.customShortCode = customShortCode; }
    
    public Integer getExpirationDays() { return expirationDays; }
    public void setExpirationDays(Integer expirationDays) { this.expirationDays = expirationDays; }
}