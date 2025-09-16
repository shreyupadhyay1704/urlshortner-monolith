package com.intellias.urlapp.application.usecases;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import com.intellias.urlapp.domain.services.UrlShorteningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortenUrlUseCase Tests")
class ShortenUrlUseCaseTest {

    @Mock
    private UrlShorteningService urlShorteningService;

    @Mock
    private UrlMappingRepository urlMappingRepository;

    private ShortenUrlUseCase shortenUrlUseCase;

    private static final String ORIGINAL_URL = "https://example.com";
    private static final String SHORT_CODE = "abc123";
    private static final String CUSTOM_SHORT_CODE = "custom";

    @BeforeEach
    void setUp() {
        shortenUrlUseCase = new ShortenUrlUseCase(urlShorteningService, urlMappingRepository);
    }

    @Nested
    @DisplayName("Execute Method Tests")
    class ExecuteMethodTests {

        @Test
        @DisplayName("Should create new shortened URL when URL does not exist")
        void shouldCreateNewShortenedUrlWhenUrlDoesNotExist() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);
            
            UrlMapping newMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);
            UrlMapping savedMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, null, null)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping)).thenReturn(savedMapping);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            
            verify(urlMappingRepository).findByOriginalUrl(ORIGINAL_URL);
            verify(urlShorteningService).createShortenedUrl(ORIGINAL_URL, null, null);
            verify(urlMappingRepository).save(newMapping);
        }

        @Test
        @DisplayName("Should return existing mapping when URL exists and is active")
        void shouldReturnExistingMappingWhenUrlExistsAndIsActive() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);
            
            UrlMapping existingMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7), 5, LocalDateTime.now());

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(existingMapping));

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            
            verify(urlMappingRepository).findByOriginalUrl(ORIGINAL_URL);
            verify(urlShorteningService, never()).createShortenedUrl(anyString(), anyString(), any());
            verify(urlMappingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new mapping when existing URL is expired")
        void shouldCreateNewMappingWhenExistingUrlIsExpired() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);
            
            UrlMapping expiredMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1), 5, LocalDateTime.now());
            UrlMapping newMapping = new UrlMapping("new123", ORIGINAL_URL, null);
            UrlMapping savedMapping = new UrlMapping("new123", ORIGINAL_URL, null);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.of(expiredMapping));
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, null, null)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping)).thenReturn(savedMapping);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo("new123");
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            
            verify(urlMappingRepository).findByOriginalUrl(ORIGINAL_URL);
            verify(urlShorteningService).createShortenedUrl(ORIGINAL_URL, null, null);
            verify(urlMappingRepository).save(newMapping);
        }

        @Test
        @DisplayName("Should create shortened URL with custom short code")
        void shouldCreateShortenedUrlWithCustomShortCode() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, CUSTOM_SHORT_CODE, null);
            
            UrlMapping newMapping = new UrlMapping(CUSTOM_SHORT_CODE, ORIGINAL_URL, null);
            UrlMapping savedMapping = new UrlMapping(CUSTOM_SHORT_CODE, ORIGINAL_URL, null);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, CUSTOM_SHORT_CODE, null)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping)).thenReturn(savedMapping);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(CUSTOM_SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            
            verify(urlShorteningService).createShortenedUrl(ORIGINAL_URL, CUSTOM_SHORT_CODE, null);
        }

        @Test
        @DisplayName("Should create shortened URL with expiration days")
        void shouldCreateShortenedUrlWithExpirationDays() {
            // Given
            Integer expirationDays = 30;
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, expirationDays);
            
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(expirationDays);
            UrlMapping newMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, expirationDate);
            UrlMapping savedMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, expirationDate);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, null, expirationDays)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping)).thenReturn(savedMapping);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getExpiresAt()).isNotNull();
            
            verify(urlShorteningService).createShortenedUrl(ORIGINAL_URL, null, expirationDays);
        }

        @Test
        @DisplayName("Should create shortened URL with custom short code and expiration")
        void shouldCreateShortenedUrlWithCustomShortCodeAndExpiration() {
            // Given
            Integer expirationDays = 7;
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, CUSTOM_SHORT_CODE, expirationDays);
            
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(expirationDays);
            UrlMapping newMapping = new UrlMapping(CUSTOM_SHORT_CODE, ORIGINAL_URL, expirationDate);
            UrlMapping savedMapping = new UrlMapping(CUSTOM_SHORT_CODE, ORIGINAL_URL, expirationDate);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, CUSTOM_SHORT_CODE, expirationDays)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping)).thenReturn(savedMapping);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = shortenUrlUseCase.execute(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getShortCode()).isEqualTo(CUSTOM_SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getExpiresAt()).isNotNull();
            
            verify(urlShorteningService).createShortenedUrl(ORIGINAL_URL, CUSTOM_SHORT_CODE, expirationDays);
        }
    }

    @Nested
    @DisplayName("Request Object Tests")
    class RequestObjectTests {

        @Test
        @DisplayName("Should create request with all parameters")
        void shouldCreateRequestWithAllParameters() {
            // When
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, CUSTOM_SHORT_CODE, 30);

            // Then
            assertThat(request.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(request.getCustomShortCode()).isEqualTo(CUSTOM_SHORT_CODE);
            assertThat(request.getExpirationDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should create request with minimal parameters")
        void shouldCreateRequestWithMinimalParameters() {
            // When
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);

            // Then
            assertThat(request.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(request.getCustomShortCode()).isNull();
            assertThat(request.getExpirationDays()).isNull();
        }
    }

    @Nested
    @DisplayName("Response Object Tests")
    class ResponseObjectTests {

        @Test
        @DisplayName("Should create response from URL mapping")
        void shouldCreateResponseFromUrlMapping() {
            // Given
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(7);
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, expirationDate);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = 
                ShortenUrlUseCase.ShortenUrlResponse.fromUrlMapping(mapping);

            // Then
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getExpiresAt()).isEqualTo(expirationDate.toString());
            assertThat(response.getShortUrl()).isNull(); // Set in presentation layer
            assertThat(response.isCustom()).isFalse(); // Default value
        }

        @Test
        @DisplayName("Should create response from URL mapping without expiration")
        void shouldCreateResponseFromUrlMappingWithoutExpiration() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When
            ShortenUrlUseCase.ShortenUrlResponse response = 
                ShortenUrlUseCase.ShortenUrlResponse.fromUrlMapping(mapping);

            // Then
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getExpiresAt()).isNull();
        }

        @Test
        @DisplayName("Should create response with all parameters")
        void shouldCreateResponseWithAllParameters() {
            // When
            ShortenUrlUseCase.ShortenUrlResponse response = 
                new ShortenUrlUseCase.ShortenUrlResponse(
                    SHORT_CODE, ORIGINAL_URL, "http://short.ly/abc123", true, "2025-10-01T10:00:00");

            // Then
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getShortUrl()).isEqualTo("http://short.ly/abc123");
            assertThat(response.isCustom()).isTrue();
            assertThat(response.getExpiresAt()).isEqualTo("2025-10-01T10:00:00");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should propagate repository exceptions")
        void shouldPropagateRepositoryExceptions() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);
            
            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL))
                .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> shortenUrlUseCase.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should propagate service exceptions")
        void shouldPropagateServiceExceptions() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, CUSTOM_SHORT_CODE, null);
            
            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, CUSTOM_SHORT_CODE, null))
                .thenThrow(new IllegalArgumentException("Custom short code already exists"));

            // When & Then
            assertThatThrownBy(() -> shortenUrlUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Custom short code already exists");
        }

        @Test
        @DisplayName("Should handle save operation failures")
        void shouldHandleSaveOperationFailures() {
            // Given
            ShortenUrlUseCase.ShortenUrlRequest request = 
                new ShortenUrlUseCase.ShortenUrlRequest(ORIGINAL_URL, null, null);
            
            UrlMapping newMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            when(urlMappingRepository.findByOriginalUrl(ORIGINAL_URL)).thenReturn(Optional.empty());
            when(urlShorteningService.createShortenedUrl(ORIGINAL_URL, null, null)).thenReturn(newMapping);
            when(urlMappingRepository.save(newMapping))
                .thenThrow(new RuntimeException("Failed to save"));

            // When & Then
            assertThatThrownBy(() -> shortenUrlUseCase.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to save");
        }
    }
}