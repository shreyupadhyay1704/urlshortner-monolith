package com.intellias.urlapp.domain;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;

public class UrlRequest {
    @NotBlank
    @URL(message = "Invalid URL format")
    private String fullUrl;

    public String getFullUrl() { return fullUrl; }
    public void setFullUrl(String fullUrl) { this.fullUrl = fullUrl; }
}

