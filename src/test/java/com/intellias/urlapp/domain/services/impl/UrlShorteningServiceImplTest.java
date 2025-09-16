package com.intellias.urlapp.domain.services.impl;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShorteningServiceImpl Tests")
class UrlShorteningServiceImplTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    private UrlShorteningServiceImpl urlShorteningService;

    private static final String VALID_ORIGINAL_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        urlShorteningService = new UrlShorteningServiceImpl(urlMappingRepository);
    }

    @Nested
    @DisplayName("Generate Short Code Tests")
    class GenerateShortCodeTests {

        @Test
        @DisplayName("Should generate short code of correct length")
        void shouldGenerateShortCodeOfCorrectLength() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode = urlShorteningService.generateShortCode(VALID_ORIGINAL_URL);

            // Then
            assertThat(shortCode)
                .hasSize(6)
                .matches("[a-zA-Z0-9]+");
        }

        @Test
        @DisplayName("Should generate unique short code")
        void shouldGenerateUniqueShortCode() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString()))
                .thenReturn(true)  // First attempt exists
                .thenReturn(false); // Second attempt is unique

            // When
            String shortCode = urlShorteningService.generateShortCode(VALID_ORIGINAL_URL);

            // Then
            assertThat(shortCode).isNotNull().hasSize(6);
            verify(urlMappingRepository, atLeast(2)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("Should throw exception when unable to generate unique code after max attempts")
        void shouldThrowExceptionWhenUnableToGenerateUniqueCode() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> urlShorteningService.generateShortCode(VALID_ORIGINAL_URL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to generate unique short code after 10 attempts");
            
            verify(urlMappingRepository, times(10)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("Should generate different codes on multiple calls")
        void shouldGenerateDifferentCodesOnMultipleCalls() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode1 = urlShorteningService.generateShortCode(VALID_ORIGINAL_URL);
            String shortCode2 = urlShorteningService.generateShortCode(VALID_ORIGINAL_URL);

            // Then
            // Note: There's a very small chance they could be the same, but very unlikely
            assertThat(shortCode1).isNotEqualTo(shortCode2);
        }
    }

    @Nested
    @DisplayName("Short Code Availability Tests")
    class ShortCodeAvailabilityTests {

        @Test
        @DisplayName("Should return true when short code is available")
        void shouldReturnTrueWhenShortCodeIsAvailable() {
            // Given
            String shortCode = "abc123";
            when(urlMappingRepository.existsByShortCode(shortCode)).thenReturn(false);

            // When
            boolean available = urlShorteningService.isShortCodeAvailable(shortCode);

            // Then
            assertThat(available).isTrue();
            verify(urlMappingRepository).existsByShortCode(shortCode);
        }

        @Test
        @DisplayName("Should return false when short code is not available")
        void shouldReturnFalseWhenShortCodeIsNotAvailable() {
            // Given
            String shortCode = "abc123";
            when(urlMappingRepository.existsByShortCode(shortCode)).thenReturn(true);

            // When
            boolean available = urlShorteningService.isShortCodeAvailable(shortCode);

            // Then
            assertThat(available).isFalse();
            verify(urlMappingRepository).existsByShortCode(shortCode);
        }
    }

    @Nested
    @DisplayName("Create Shortened URL Tests")
    class CreateShortenedUrlTests {

        @Test
        @DisplayName("Should create URL mapping with generated short code")
        void shouldCreateUrlMappingWithGeneratedShortCode() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(VALID_ORIGINAL_URL, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalUrl()).isEqualTo(VALID_ORIGINAL_URL);
            assertThat(result.getShortCode()).isNotNull().hasSize(6);
            assertThat(result.getExpiresAt()).isNull();
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create URL mapping with custom short code")
        void shouldCreateUrlMappingWithCustomShortCode() {
            // Given
            String customShortCode = "custom";
            when(urlMappingRepository.existsByShortCode(customShortCode)).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, customShortCode, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalUrl()).isEqualTo(VALID_ORIGINAL_URL);
            assertThat(result.getShortCode()).isEqualTo(customShortCode);
            assertThat(result.getExpiresAt()).isNull();
        }

        @Test
        @DisplayName("Should create URL mapping with expiration")
        void shouldCreateUrlMappingWithExpiration() {
            // Given
            Integer expirationDays = 7;
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            LocalDateTime beforeCreation = LocalDateTime.now().plusDays(expirationDays);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, null, expirationDays);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getExpiresAt()).isNotNull();
            assertThat(result.getExpiresAt()).isAfterOrEqualTo(beforeCreation.minusSeconds(1));
            assertThat(result.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now().plusDays(expirationDays).plusSeconds(1));
        }

        @Test
        @DisplayName("Should create URL mapping with custom short code and expiration")
        void shouldCreateUrlMappingWithCustomShortCodeAndExpiration() {
            // Given
            String customShortCode = "custom";
            Integer expirationDays = 30;
            when(urlMappingRepository.existsByShortCode(customShortCode)).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, customShortCode, expirationDays);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShortCode()).isEqualTo(customShortCode);
            assertThat(result.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should trim whitespace from custom short code")
        void shouldTrimWhitespaceFromCustomShortCode() {
            // Given
            String customShortCodeWithSpaces = "  custom  ";
            when(urlMappingRepository.existsByShortCode("custom")).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, customShortCodeWithSpaces, null);

            // Then
            assertThat(result.getShortCode()).isEqualTo("custom");
            verify(urlMappingRepository).existsByShortCode("custom");
        }

        @Test
        @DisplayName("Should throw exception when custom short code is already in use")
        void shouldThrowExceptionWhenCustomShortCodeIsAlreadyInUse() {
            // Given
            String existingShortCode = "existing";
            when(urlMappingRepository.existsByShortCode(existingShortCode)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, existingShortCode, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Custom short code 'existing' is already in use");
        }

        @Test
        @DisplayName("Should ignore empty custom short code and generate one")
        void shouldIgnoreEmptyCustomShortCodeAndGenerateOne() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, "", null);

            // Then
            assertThat(result.getShortCode()).isNotEmpty().hasSize(6);
        }

        @Test
        @DisplayName("Should ignore whitespace-only custom short code and generate one")
        void shouldIgnoreWhitespaceOnlyCustomShortCodeAndGenerateOne() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, "   ", null);

            // Then
            assertThat(result.getShortCode()).isNotEmpty().hasSize(6);
        }

        @Test
        @DisplayName("Should ignore zero or negative expiration days")
        void shouldIgnoreZeroOrNegativeExpirationDays() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            UrlMapping result1 = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, null, 0);
            UrlMapping result2 = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, null, -5);

            // Then
            assertThat(result1.getExpiresAt()).isNull();
            assertThat(result2.getExpiresAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString()))
                .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> urlShorteningService.generateShortCode(VALID_ORIGINAL_URL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should validate URL mapping creation with domain rules")
        void shouldValidateUrlMappingCreationWithDomainRules() {
            // Given
            String customShortCode = "valid";
            when(urlMappingRepository.existsByShortCode(customShortCode)).thenReturn(false);

            // When
            UrlMapping result = urlShorteningService.createShortenedUrl(
                VALID_ORIGINAL_URL, customShortCode, 7);

            // Then
            assertThat(result.getShortCode()).isEqualTo(customShortCode);
            assertThat(result.getOriginalUrl()).isEqualTo(VALID_ORIGINAL_URL);
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getClickCount()).isZero();
            assertThat(result.getLastAccessedAt()).isNull();
            assertThat(result.isActive()).isTrue();
        }
    }
}