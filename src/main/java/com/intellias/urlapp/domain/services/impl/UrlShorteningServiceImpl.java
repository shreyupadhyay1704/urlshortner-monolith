package com.intellias.urlapp.domain.services.impl;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.services.UrlShorteningService;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Implementation of URL shortening domain service.
 * Contains the core business logic for URL shortening operations.
 */
public class UrlShorteningServiceImpl implements UrlShorteningService {
    
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int DEFAULT_SHORT_CODE_LENGTH = 6;
    private static final int MAX_RETRY_ATTEMPTS = 10;
    
    private final UrlMappingRepository urlMappingRepository;
    private final Random random;
    
    public UrlShorteningServiceImpl(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.random = new Random();
    }
    
    @Override
    public String generateShortCode(String originalUrl) {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            String shortCode = generateRandomShortCode();
            if (isShortCodeAvailable(shortCode)) {
                return shortCode;
            }
        }
        throw new RuntimeException("Unable to generate unique short code after " + MAX_RETRY_ATTEMPTS + " attempts");
    }
    
    @Override
    public boolean isShortCodeAvailable(String shortCode) {
        return !urlMappingRepository.existsByShortCode(shortCode);
    }
    
    @Override
    public UrlMapping createShortenedUrl(String originalUrl, String customShortCode, Integer expirationDays) {
        String shortCode;
        
        if (customShortCode != null && !customShortCode.trim().isEmpty()) {
            String trimmedShortCode = customShortCode.trim();
            if (!isShortCodeAvailable(trimmedShortCode)) {
                throw new IllegalArgumentException("Custom short code '" + trimmedShortCode + "' is already in use");
            }
            shortCode = trimmedShortCode;
        } else {
            shortCode = generateShortCode(originalUrl);
        }
        
        LocalDateTime expiresAt = null;
        if (expirationDays != null && expirationDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(expirationDays);
        }
        
        return new UrlMapping(shortCode, originalUrl, expiresAt);
    }
    
    private String generateRandomShortCode() {
        StringBuilder shortCode = new StringBuilder();
        for (int i = 0; i < DEFAULT_SHORT_CODE_LENGTH; i++) {
            shortCode.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return shortCode.toString();
    }
}