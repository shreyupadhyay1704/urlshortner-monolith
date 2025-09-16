package com.intellias.urlapp.presentation.controllers;

import com.intellias.urlapp.application.usecases.ShortenUrlUseCase;
import com.intellias.urlapp.application.usecases.RetrieveUrlUseCase;
import com.intellias.urlapp.application.usecases.AnalyticsUseCase;
import com.intellias.urlapp.presentation.dto.ShortenUrlRequestDto;
import com.intellias.urlapp.presentation.dto.ShortenUrlResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * REST controller for URL operations.
 * This is the presentation layer that handles HTTP requests and responses.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class UrlController {
    
    private final ShortenUrlUseCase shortenUrlUseCase;
    private final RetrieveUrlUseCase retrieveUrlUseCase;
    private final AnalyticsUseCase analyticsUseCase;
    
    public UrlController(ShortenUrlUseCase shortenUrlUseCase,
                        RetrieveUrlUseCase retrieveUrlUseCase,
                        AnalyticsUseCase analyticsUseCase) {
        this.shortenUrlUseCase = shortenUrlUseCase;
        this.retrieveUrlUseCase = retrieveUrlUseCase;
        this.analyticsUseCase = analyticsUseCase;
    }
    
    /**
     * Creates a shortened URL
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponseDto> shortenUrl(@Valid @RequestBody ShortenUrlRequestDto request) {
        try {
            ShortenUrlUseCase.ShortenUrlRequest useCaseRequest = 
                new ShortenUrlUseCase.ShortenUrlRequest(
                    request.getOriginalUrl(),
                    request.getCustomShortCode(),
                    request.getExpirationDays()
                );
            
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = shortenUrlUseCase.execute(useCaseRequest);
            
            // Construct short URL in presentation layer using request context
            String baseUrl = "http://localhost:5000"; // In production, this would come from configuration or request
            String shortUrl = baseUrl + "/" + useCaseResponse.getShortCode();
            
            ShortenUrlResponseDto response = new ShortenUrlResponseDto(
                useCaseResponse.getShortCode(),
                useCaseResponse.getOriginalUrl(),
                shortUrl,
                useCaseResponse.isCustom(),
                useCaseResponse.getExpiresAt(),
                "URL shortened successfully"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ShortenUrlResponseDto errorResponse = new ShortenUrlResponseDto();
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            ShortenUrlResponseDto errorResponse = new ShortenUrlResponseDto();
            errorResponse.setMessage("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Gets URL analytics for a specific short code
     */
    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<AnalyticsUseCase.UrlAnalyticsResponse> getUrlAnalytics(@PathVariable String shortCode) {
        try {
            // Validate short code
            if (shortCode == null || shortCode.trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            AnalyticsUseCase.UrlAnalyticsResponse analytics = analyticsUseCase.getUrlAnalytics(shortCode);
            
            // Handle null response defensively
            if (analytics == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            if (!analytics.isFound()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Gets system-wide analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsUseCase.SystemAnalyticsResponse> getSystemAnalytics() {
        try {
            AnalyticsUseCase.SystemAnalyticsResponse analytics = analyticsUseCase.getSystemAnalytics();
            
            // Handle null response defensively
            if (analytics == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

