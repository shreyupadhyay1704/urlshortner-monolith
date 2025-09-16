package com.intellias.urlapp.application.usecases;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetrieveUrlUseCase Tests")
class RetrieveUrlUseCaseTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    private RetrieveUrlUseCase retrieveUrlUseCase;

    private static final String SHORT_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        retrieveUrlUseCase = new RetrieveUrlUseCase(urlMappingRepository);
    }

    @Nested
    @DisplayName("Execute Method Tests")
    class ExecuteMethodTests {

        @Test
        @DisplayName("Should return original URL and record access for valid short code")
        void shouldReturnOriginalUrlAndRecordAccessForValidShortCode() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7), 5, null);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class))).thenReturn(mapping);

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.isFound()).isTrue();
            assertThat(response.isExpired()).isFalse();
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getMessage()).isEqualTo("URL found");
            
            // Verify that access was recorded
            assertThat(mapping.getClickCount()).isEqualTo(6); // Incremented from 5 to 6
            assertThat(mapping.getLastAccessedAt()).isNotNull();
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository).updateClickStats(mapping);
        }

        @Test
        @DisplayName("Should return not found response for non-existent short code")
        void shouldReturnNotFoundResponseForNonExistentShortCode() {
            // Given
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.empty());

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isFound()).isFalse();
            assertThat(response.isExpired()).isFalse();
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.getMessage()).isEqualTo("Short URL not found");
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository, never()).updateClickStats(any());
        }

        @Test
        @DisplayName("Should return expired response for expired URL")
        void shouldReturnExpiredResponseForExpiredUrl() {
            // Given
            UrlMapping expiredMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1), 5, null);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(expiredMapping));

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isFound()).isFalse();
            assertThat(response.isExpired()).isTrue();
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.getMessage()).isEqualTo("Short URL has expired");
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository, never()).updateClickStats(any());
        }

        @Test
        @DisplayName("Should handle URL with no expiration")
        void shouldHandleUrlWithNoExpiration() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class))).thenReturn(mapping);

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.isFound()).isTrue();
            assertThat(response.isExpired()).isFalse();
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            
            verify(urlMappingRepository).updateClickStats(mapping);
        }

        @Test
        @DisplayName("Should handle URL that expires exactly now")
        void shouldHandleUrlThatExpiresExactlyNow() {
            // Given - URL that expires in the past (even by nanoseconds)
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusNanos(1));
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isExpired()).isTrue();
            assertThat(response.getOriginalUrl()).isNull();
            
            verify(urlMappingRepository, never()).updateClickStats(any());
        }

        @Test
        @DisplayName("Should increment click count from zero")
        void shouldIncrementClickCountFromZero() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().plusDays(1));
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class))).thenReturn(mapping);

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response.isSuccessful()).isTrue();
            assertThat(mapping.getClickCount()).isEqualTo(1);
            assertThat(mapping.getLastAccessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should record access time correctly")
        void shouldRecordAccessTimeCorrectly() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().plusDays(1));
            LocalDateTime beforeAccess = LocalDateTime.now();
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class))).thenReturn(mapping);

            // When
            retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(mapping.getLastAccessedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeAccess)
                .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("Response Object Tests")
    class ResponseObjectTests {

        @Test
        @DisplayName("Should create success response correctly")
        void shouldCreateSuccessResponseCorrectly() {
            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            // Then
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.isFound()).isTrue();
            assertThat(response.isExpired()).isFalse();
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.getMessage()).isEqualTo("URL found");
        }

        @Test
        @DisplayName("Should create not found response correctly")
        void shouldCreateNotFoundResponseCorrectly() {
            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = 
                RetrieveUrlUseCase.RetrieveUrlResponse.notFound();

            // Then
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.isFound()).isFalse();
            assertThat(response.isExpired()).isFalse();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Short URL not found");
        }

        @Test
        @DisplayName("Should create expired response correctly")
        void shouldCreateExpiredResponseCorrectly() {
            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = 
                RetrieveUrlUseCase.RetrieveUrlResponse.expired();

            // Then
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.isFound()).isFalse();
            assertThat(response.isExpired()).isTrue();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Short URL has expired");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should propagate repository find exceptions")
        void shouldPropagateRepositoryFindExceptions() {
            // Given
            when(urlMappingRepository.findByShortCode(SHORT_CODE))
                .thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThatThrownBy(() -> retrieveUrlUseCase.execute(SHORT_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository, never()).updateClickStats(any());
        }

        @Test
        @DisplayName("Should propagate repository update exceptions")
        void shouldPropagateRepositoryUpdateExceptions() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().plusDays(1));
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class)))
                .thenThrow(new RuntimeException("Failed to update click stats"));

            // When & Then
            assertThatThrownBy(() -> retrieveUrlUseCase.execute(SHORT_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to update click stats");
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository).updateClickStats(mapping);
        }

        @Test
        @DisplayName("Should handle null short code gracefully")
        void shouldHandleNullShortCodeGracefully() {
            // Given
            when(urlMappingRepository.findByShortCode(null)).thenReturn(Optional.empty());

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(null);

            // Then
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isFound()).isFalse();
            
            verify(urlMappingRepository).findByShortCode(null);
        }

        @Test
        @DisplayName("Should handle empty short code gracefully")
        void shouldHandleEmptyShortCodeGracefully() {
            // Given
            when(urlMappingRepository.findByShortCode("")).thenReturn(Optional.empty());

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute("");

            // Then
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isFound()).isFalse();
            
            verify(urlMappingRepository).findByShortCode("");
        }
    }

    @Nested
    @DisplayName("Click Tracking Integration Tests")
    class ClickTrackingIntegrationTests {

        @Test
        @DisplayName("Should record multiple accesses correctly")
        void shouldRecordMultipleAccessesCorrectly() {
            // Given
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().plusDays(1));
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));
            when(urlMappingRepository.updateClickStats(any(UrlMapping.class))).thenReturn(mapping);

            // When - Access multiple times
            retrieveUrlUseCase.execute(SHORT_CODE);
            retrieveUrlUseCase.execute(SHORT_CODE);
            retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(mapping.getClickCount()).isEqualTo(3);
            verify(urlMappingRepository, times(3)).findByShortCode(SHORT_CODE);
            verify(urlMappingRepository, times(3)).updateClickStats(mapping);
        }

        @Test
        @DisplayName("Should not record access for expired URLs")
        void shouldNotRecordAccessForExpiredUrls() {
            // Given
            UrlMapping expiredMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(1));
            int originalClickCount = expiredMapping.getClickCount();
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(expiredMapping));

            // When
            RetrieveUrlUseCase.RetrieveUrlResponse response = retrieveUrlUseCase.execute(SHORT_CODE);

            // Then
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isExpired()).isTrue();
            assertThat(expiredMapping.getClickCount()).isEqualTo(originalClickCount); // No increment
            
            verify(urlMappingRepository, never()).updateClickStats(any());
        }
    }
}