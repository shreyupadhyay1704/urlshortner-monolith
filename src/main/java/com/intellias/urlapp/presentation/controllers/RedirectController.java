package com.intellias.urlapp.presentation.controllers;

import com.intellias.urlapp.application.usecases.RetrieveUrlUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for URL redirection to keep concerns separated
 */
@RestController
public class RedirectController {
    
    private final RetrieveUrlUseCase retrieveUrlUseCase;
    
    public RedirectController(RetrieveUrlUseCase retrieveUrlUseCase) {
        this.retrieveUrlUseCase = retrieveUrlUseCase;
    }
    
    /**
     * Redirects short URL to original URL
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        try {
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(shortCode);
            
            // Handle null response defensively
            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal server error");
            }
            
            // Check expiration first - expired URLs should return 410 even if found=false
            if (response.isExpired()) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body("Short URL has expired");
            }
            
            if (!response.isFound()) {
                return ResponseEntity.notFound().build();
            }
            
            RedirectView redirectView = new RedirectView(response.getOriginalUrl());
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header("Location", response.getOriginalUrl())
                    .build();
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }
    }
}