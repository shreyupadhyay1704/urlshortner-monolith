package com.intellias.urlapp.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellias.urlapp.application.usecases.AnalyticsUseCase;
import com.intellias.urlapp.application.usecases.RetrieveUrlUseCase;
import com.intellias.urlapp.application.usecases.ShortenUrlUseCase;
import com.intellias.urlapp.presentation.dto.ShortenUrlRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@ActiveProfiles("test")
@DisplayName("UrlController Tests")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortenUrlUseCase shortenUrlUseCase;

    @MockBean
    private RetrieveUrlUseCase retrieveUrlUseCase;

    @MockBean
    private AnalyticsUseCase analyticsUseCase;

    private static final String ORIGINAL_URL = "https://example.com";
    private static final String SHORT_CODE = "abc123";
    private static final String SHORT_URL = "http://localhost:5000/abc123";

    @Nested
    @DisplayName("POST /api/v1/shorten - Shorten URL Endpoint")
    class ShortenUrlEndpoint {

        @Test
        @DisplayName("Should successfully shorten URL with minimal request")
        void shouldSuccessfullyShortenUrlWithMinimalRequest() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(SHORT_CODE, ORIGINAL_URL, null, false, null);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode", is(SHORT_CODE)))
                .andExpect(jsonPath("$.originalUrl", is(ORIGINAL_URL)))
                .andExpect(jsonPath("$.shortUrl", is(SHORT_URL)))
                .andExpect(jsonPath("$.custom", is(false)))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.message", is("URL shortened successfully")));

            verify(shortenUrlUseCase).execute(any(ShortenUrlUseCase.ShortenUrlRequest.class));
        }

        @Test
        @DisplayName("Should successfully shorten URL with custom short code")
        void shouldSuccessfullyShortenUrlWithCustomShortCode() throws Exception {
            // Given
            String customShortCode = "custom";
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, customShortCode, null);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(customShortCode, ORIGINAL_URL, null, true, null);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(customShortCode)))
                .andExpect(jsonPath("$.custom", is(true)))
                .andExpect(jsonPath("$.shortUrl", is("http://localhost:5000/" + customShortCode)));
        }

        @Test
        @DisplayName("Should successfully shorten URL with expiration")
        void shouldSuccessfullyShortenUrlWithExpiration() throws Exception {
            // Given
            Integer expirationDays = 30;
            String expirationDate = "2025-10-15T10:00:00";
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, expirationDays);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(SHORT_CODE, ORIGINAL_URL, null, false, expirationDate);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt", is(expirationDate)));
        }

        @Test
        @DisplayName("Should return 400 for missing original URL")
        void shouldReturn400ForMissingOriginalUrl() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(null, null, null);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 400 for empty original URL")
        void shouldReturn400ForEmptyOriginalUrl() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto("", null, null);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid URL format")
        void shouldReturn400ForInvalidUrlFormat() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto("invalid-url", null, null);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 400 for negative expiration days")
        void shouldReturn400ForNegativeExpirationDays() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, -5);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 400 for zero expiration days")
        void shouldReturn400ForZeroExpirationDays() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, 0);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 400 when use case throws IllegalArgumentException")
        void shouldReturn400WhenUseCaseThrowsIllegalArgumentException() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, "existing", null);
            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenThrow(new IllegalArgumentException("Custom short code already exists"));

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Custom short code already exists")));

            verify(shortenUrlUseCase).execute(any(ShortenUrlUseCase.ShortenUrlRequest.class));
        }

        @Test
        @DisplayName("Should return 500 for internal server error")
        void shouldReturn500ForInternalServerError() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Internal server error")));

            verify(shortenUrlUseCase).execute(any(ShortenUrlUseCase.ShortenUrlRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

            verify(shortenUrlUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should return 415 for unsupported media type")
        void shouldReturn415ForUnsupportedMediaType() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

            verify(shortenUrlUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/{shortCode} - URL Analytics Endpoint")
    class UrlAnalyticsEndpoint {

        @Test
        @DisplayName("Should return analytics for existing URL")
        void shouldReturnAnalyticsForExistingUrl() throws Exception {
            // Given
            AnalyticsUseCase.UrlAnalyticsResponse analyticsResponse = 
                new AnalyticsUseCase.UrlAnalyticsResponse(SHORT_CODE, ORIGINAL_URL, 25, 
                    "2025-09-10T10:00:00", "2025-09-14T15:30:00", true, null, true);

            when(analyticsUseCase.getUrlAnalytics(SHORT_CODE)).thenReturn(analyticsResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", SHORT_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode", is(SHORT_CODE)))
                .andExpect(jsonPath("$.originalUrl", is(ORIGINAL_URL)))
                .andExpect(jsonPath("$.clickCount", is(25)))
                .andExpect(jsonPath("$.createdAt", is("2025-09-10T10:00:00")))
                .andExpect(jsonPath("$.lastAccessedAt", is("2025-09-14T15:30:00")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.found", is(true)));

            verify(analyticsUseCase).getUrlAnalytics(SHORT_CODE);
        }

        @Test
        @DisplayName("Should return 404 for non-existent URL")
        void shouldReturn404ForNonExistentUrl() throws Exception {
            // Given
            AnalyticsUseCase.UrlAnalyticsResponse notFoundResponse = 
                AnalyticsUseCase.UrlAnalyticsResponse.notFound();

            when(analyticsUseCase.getUrlAnalytics("nonexistent")).thenReturn(notFoundResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", "nonexistent"))
                .andExpect(status().isNotFound());

            verify(analyticsUseCase).getUrlAnalytics("nonexistent");
        }

        @Test
        @DisplayName("Should handle analytics for expired URL")
        void shouldHandleAnalyticsForExpiredUrl() throws Exception {
            // Given
            AnalyticsUseCase.UrlAnalyticsResponse analyticsResponse = 
                new AnalyticsUseCase.UrlAnalyticsResponse(SHORT_CODE, ORIGINAL_URL, 10, 
                    "2025-09-01T10:00:00", "2025-09-10T15:30:00", false, "2025-09-10T10:00:00", true);

            when(analyticsUseCase.getUrlAnalytics(SHORT_CODE)).thenReturn(analyticsResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", SHORT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.expiresAt", is("2025-09-10T10:00:00")));

            verify(analyticsUseCase).getUrlAnalytics(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle analytics service exception")
        void shouldHandleAnalyticsServiceException() throws Exception {
            // Given
            when(analyticsUseCase.getUrlAnalytics(SHORT_CODE))
                .thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", SHORT_CODE))
                .andExpect(status().isInternalServerError());

            verify(analyticsUseCase).getUrlAnalytics(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle empty short code")
        void shouldHandleEmptyShortCode() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/analytics/ "))
                .andExpect(status().isNotFound()); // Spring treats empty path variable as 404

            verify(analyticsUseCase, never()).getUrlAnalytics(anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics - System Analytics Endpoint")
    class SystemAnalyticsEndpoint {

        @Test
        @DisplayName("Should return comprehensive system analytics")
        void shouldReturnComprehensiveSystemAnalytics() throws Exception {
            // Given
            AnalyticsUseCase.UrlAnalyticsResponse topUrl1 = 
                new AnalyticsUseCase.UrlAnalyticsResponse("top1", "https://example1.com", 100, 
                    "2025-09-01T10:00:00", "2025-09-14T15:30:00", true, null, true);
            AnalyticsUseCase.UrlAnalyticsResponse topUrl2 = 
                new AnalyticsUseCase.UrlAnalyticsResponse("top2", "https://example2.com", 75, 
                    "2025-09-02T10:00:00", "2025-09-14T14:20:00", true, null, true);

            AnalyticsUseCase.SystemAnalyticsResponse systemResponse = 
                new AnalyticsUseCase.SystemAnalyticsResponse(150, 500, 120, 30, Arrays.asList(topUrl1, topUrl2));

            when(analyticsUseCase.getSystemAnalytics()).thenReturn(systemResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalUrls", is(150)))
                .andExpect(jsonPath("$.totalClicks", is(500)))
                .andExpect(jsonPath("$.activeUrls", is(120)))
                .andExpect(jsonPath("$.expiredUrls", is(30)))
                .andExpect(jsonPath("$.topUrls", hasSize(2)))
                .andExpect(jsonPath("$.topUrls[0].shortCode", is("top1")))
                .andExpect(jsonPath("$.topUrls[0].clickCount", is(100)))
                .andExpect(jsonPath("$.topUrls[1].shortCode", is("top2")))
                .andExpect(jsonPath("$.topUrls[1].clickCount", is(75)));

            verify(analyticsUseCase).getSystemAnalytics();
        }

        @Test
        @DisplayName("Should handle empty system (no URLs)")
        void shouldHandleEmptySystem() throws Exception {
            // Given
            AnalyticsUseCase.SystemAnalyticsResponse emptyResponse = 
                new AnalyticsUseCase.SystemAnalyticsResponse(0, 0, 0, 0, Collections.emptyList());

            when(analyticsUseCase.getSystemAnalytics()).thenReturn(emptyResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(0)))
                .andExpect(jsonPath("$.totalClicks", is(0)))
                .andExpect(jsonPath("$.activeUrls", is(0)))
                .andExpect(jsonPath("$.expiredUrls", is(0)))
                .andExpect(jsonPath("$.topUrls", hasSize(0)));

            verify(analyticsUseCase).getSystemAnalytics();
        }

        @Test
        @DisplayName("Should handle system analytics service exception")
        void shouldHandleSystemAnalyticsServiceException() throws Exception {
            // Given
            when(analyticsUseCase.getSystemAnalytics())
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isInternalServerError());

            verify(analyticsUseCase).getSystemAnalytics();
        }

        @Test
        @DisplayName("Should handle large system analytics efficiently")
        void shouldHandleLargeSystemAnalyticsEfficiently() throws Exception {
            // Given
            AnalyticsUseCase.SystemAnalyticsResponse largeResponse = 
                new AnalyticsUseCase.SystemAnalyticsResponse(10000, 1000000, 8500, 1500, Collections.emptyList());

            when(analyticsUseCase.getSystemAnalytics()).thenReturn(largeResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(10000)))
                .andExpect(jsonPath("$.totalClicks", is(1000000)));

            verify(analyticsUseCase).getSystemAnalytics();
        }
    }

    @Nested
    @DisplayName("CORS and Security Tests")
    class CorsAndSecurityTests {

        @Test
        @DisplayName("Should handle CORS preflight request")
        void shouldHandleCorsPreflight() throws Exception {
            // When & Then
            mockMvc.perform(options("/api/v1/shorten")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        }

        @Test
        @DisplayName("Should allow cross-origin requests")
        void shouldAllowCrossOriginRequests() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(SHORT_CODE, ORIGINAL_URL, null, false, null);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .header("Origin", "http://localhost:3000")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        }

        @Test
        @DisplayName("Should handle requests without authentication")
        void shouldHandleRequestsWithoutAuthentication() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(SHORT_CODE, ORIGINAL_URL, null, false, null);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // No authentication required

            verify(shortenUrlUseCase).execute(any(ShortenUrlUseCase.ShortenUrlRequest.class));
        }
    }

    @Nested
    @DisplayName("Content Type and Accept Header Tests")
    class ContentTypeAndAcceptHeaderTests {

        @Test
        @DisplayName("Should accept and return JSON content")
        void shouldAcceptAndReturnJsonContent() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            ShortenUrlUseCase.ShortenUrlResponse useCaseResponse = 
                new ShortenUrlUseCase.ShortenUrlResponse(SHORT_CODE, ORIGINAL_URL, null, false, null);

            when(shortenUrlUseCase.execute(any(ShortenUrlUseCase.ShortenUrlRequest.class)))
                .thenReturn(useCaseResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should handle requests with accept all media types")
        void shouldHandleRequestsWithAcceptAllMediaTypes() throws Exception {
            // Given
            when(analyticsUseCase.getUrlAnalytics(SHORT_CODE))
                .thenReturn(new AnalyticsUseCase.UrlAnalyticsResponse(SHORT_CODE, ORIGINAL_URL, 1, 
                    "2025-09-14T10:00:00", null, true, null, true));

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", SHORT_CODE)
                    .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}