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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsUseCase Tests")
class AnalyticsUseCaseTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    private AnalyticsUseCase analyticsUseCase;

    private static final String SHORT_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        analyticsUseCase = new AnalyticsUseCase(urlMappingRepository);
    }

    @Nested
    @DisplayName("URL Analytics Tests")
    class UrlAnalyticsTests {

        @Test
        @DisplayName("Should return analytics for existing URL")
        void shouldReturnAnalyticsForExistingUrl() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
            LocalDateTime lastAccessed = LocalDateTime.now().minusHours(2);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(20);
            
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 25, lastAccessed);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isFound()).isTrue();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getClickCount()).isEqualTo(25);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt.toString());
            assertThat(response.getLastAccessedAt()).isEqualTo(lastAccessed.toString());
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt.toString());
            assertThat(response.isActive()).isTrue();
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
        }

        @Test
        @DisplayName("Should return analytics for URL without expiration")
        void shouldReturnAnalyticsForUrlWithoutExpiration() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, null, 10, null);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mapping));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isFound()).isTrue();
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getClickCount()).isEqualTo(10);
            assertThat(response.getLastAccessedAt()).isNull();
            assertThat(response.getExpiresAt()).isNull();
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return analytics for expired URL")
        void shouldReturnAnalyticsForExpiredUrl() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
            LocalDateTime expiresAt = LocalDateTime.now().minusDays(1);
            LocalDateTime lastAccessed = LocalDateTime.now().minusDays(2);
            
            UrlMapping expiredMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 15, lastAccessed);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(expiredMapping));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isFound()).isTrue();
            assertThat(response.isActive()).isFalse();
            assertThat(response.getClickCount()).isEqualTo(15);
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt.toString());
        }

        @Test
        @DisplayName("Should return not found response for non-existent URL")
        void shouldReturnNotFoundResponseForNonExistentUrl() {
            // Given
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.empty());

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isFound()).isFalse();
            assertThat(response.getShortCode()).isNull();
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.getClickCount()).isZero();
            assertThat(response.getCreatedAt()).isNull();
            assertThat(response.getLastAccessedAt()).isNull();
            assertThat(response.getExpiresAt()).isNull();
            assertThat(response.isActive()).isFalse();
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle URL with zero clicks")
        void shouldHandleUrlWithZeroClicks() {
            // Given
            UrlMapping mappingWithNoClicks = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(mappingWithNoClicks));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isFound()).isTrue();
            assertThat(response.getClickCount()).isZero();
            assertThat(response.getLastAccessedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("System Analytics Tests")
    class SystemAnalyticsTests {

        @Test
        @DisplayName("Should return comprehensive system analytics")
        void shouldReturnComprehensiveSystemAnalytics() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            List<UrlMapping> mappings = Arrays.asList(
                // Active URLs
                new UrlMapping("code1", "https://example1.com", 
                    now.minusDays(5), now.plusDays(5), 100, now.minusHours(1)),
                new UrlMapping("code2", "https://example2.com", 
                    now.minusDays(3), null, 50, now.minusHours(2)),
                new UrlMapping("code3", "https://example3.com", 
                    now.minusDays(1), now.plusDays(10), 25, now.minusHours(3)),
                // Expired URLs
                new UrlMapping("code4", "https://example4.com", 
                    now.minusDays(10), now.minusDays(1), 75, now.minusDays(2)),
                new UrlMapping("code5", "https://example5.com", 
                    now.minusDays(15), now.minusDays(2), 10, now.minusDays(3))
            );
            
            when(urlMappingRepository.findAll()).thenReturn(mappings);

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalUrls()).isEqualTo(5);
            assertThat(response.getActiveUrls()).isEqualTo(3);
            assertThat(response.getExpiredUrls()).isEqualTo(2);
            assertThat(response.getTotalClicks()).isEqualTo(260); // 100+50+25+75+10
            
            // Top URLs should be sorted by click count (descending)
            List<AnalyticsUseCase.UrlAnalyticsResponse> topUrls = response.getTopUrls();
            assertThat(topUrls).hasSize(5); // All URLs since we have <= 10
            assertThat(topUrls.get(0).getShortCode()).isEqualTo("code1"); // 100 clicks
            assertThat(topUrls.get(1).getShortCode()).isEqualTo("code4"); // 75 clicks
            assertThat(topUrls.get(2).getShortCode()).isEqualTo("code2"); // 50 clicks
            assertThat(topUrls.get(3).getShortCode()).isEqualTo("code3"); // 25 clicks
            assertThat(topUrls.get(4).getShortCode()).isEqualTo("code5"); // 10 clicks
            
            verify(urlMappingRepository).findAll();
        }

        @Test
        @DisplayName("Should handle empty system (no URLs)")
        void shouldHandleEmptySystem() {
            // Given
            when(urlMappingRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalUrls()).isZero();
            assertThat(response.getActiveUrls()).isZero();
            assertThat(response.getExpiredUrls()).isZero();
            assertThat(response.getTotalClicks()).isZero();
            assertThat(response.getTopUrls()).isEmpty();
            
            verify(urlMappingRepository).findAll();
        }

        @Test
        @DisplayName("Should limit top URLs to 10")
        void shouldLimitTopUrlsToTen() {
            // Given
            List<UrlMapping> mappings = Arrays.asList(
                new UrlMapping("code01", "https://example01.com", null, null, 15, null),
                new UrlMapping("code02", "https://example02.com", null, null, 14, null),
                new UrlMapping("code03", "https://example03.com", null, null, 13, null),
                new UrlMapping("code04", "https://example04.com", null, null, 12, null),
                new UrlMapping("code05", "https://example05.com", null, null, 11, null),
                new UrlMapping("code06", "https://example06.com", null, null, 10, null),
                new UrlMapping("code07", "https://example07.com", null, null, 9, null),
                new UrlMapping("code08", "https://example08.com", null, null, 8, null),
                new UrlMapping("code09", "https://example09.com", null, null, 7, null),
                new UrlMapping("code10", "https://example10.com", null, null, 6, null),
                new UrlMapping("code11", "https://example11.com", null, null, 5, null),
                new UrlMapping("code12", "https://example12.com", null, null, 4, null)
            );
            
            when(urlMappingRepository.findAll()).thenReturn(mappings);

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response.getTotalUrls()).isEqualTo(12);
            assertThat(response.getTopUrls()).hasSize(10); // Limited to 10
            
            // Verify ordering (highest clicks first)
            List<AnalyticsUseCase.UrlAnalyticsResponse> topUrls = response.getTopUrls();
            assertThat(topUrls.get(0).getShortCode()).isEqualTo("code01"); // 15 clicks
            assertThat(topUrls.get(9).getShortCode()).isEqualTo("code10"); // 6 clicks
        }

        @Test
        @DisplayName("Should handle all URLs having zero clicks")
        void shouldHandleAllUrlsHavingZeroClicks() {
            // Given
            List<UrlMapping> mappings = Arrays.asList(
                new UrlMapping("code1", "https://example1.com", null),
                new UrlMapping("code2", "https://example2.com", null),
                new UrlMapping("code3", "https://example3.com", null)
            );
            
            when(urlMappingRepository.findAll()).thenReturn(mappings);

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response.getTotalUrls()).isEqualTo(3);
            assertThat(response.getTotalClicks()).isZero();
            assertThat(response.getActiveUrls()).isEqualTo(3);
            assertThat(response.getExpiredUrls()).isZero();
            assertThat(response.getTopUrls()).hasSize(3);
            
            // All should have zero clicks
            response.getTopUrls().forEach(urlAnalytics -> 
                assertThat(urlAnalytics.getClickCount()).isZero());
        }

        @Test
        @DisplayName("Should correctly calculate mixed active and expired URLs")
        void shouldCorrectlyCalculateMixedActiveAndExpiredUrls() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            List<UrlMapping> mappings = Arrays.asList(
                // Active (no expiration)
                new UrlMapping("active1", "https://example1.com", null),
                // Active (future expiration)
                new UrlMapping("active2", "https://example2.com", 
                    now.minusDays(1), now.plusDays(1), 0, null),
                // Expired
                new UrlMapping("expired1", "https://example3.com", 
                    now.minusDays(5), now.minusDays(1), 0, null),
                // Expired  
                new UrlMapping("expired2", "https://example4.com", 
                    now.minusDays(10), now.minusHours(1), 0, null)
            );
            
            when(urlMappingRepository.findAll()).thenReturn(mappings);

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response.getTotalUrls()).isEqualTo(4);
            assertThat(response.getActiveUrls()).isEqualTo(2);
            assertThat(response.getExpiredUrls()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Response Object Tests")
    class ResponseObjectTests {

        @Test
        @DisplayName("Should create URL analytics response from mapping correctly")
        void shouldCreateUrlAnalyticsResponseFromMappingCorrectly() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime lastAccessed = LocalDateTime.now().minusHours(1);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);
            
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 42, lastAccessed);

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = 
                AnalyticsUseCase.UrlAnalyticsResponse.fromUrlMapping(mapping);

            // Then
            assertThat(response.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(response.getClickCount()).isEqualTo(42);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt.toString());
            assertThat(response.getLastAccessedAt()).isEqualTo(lastAccessed.toString());
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt.toString());
            assertThat(response.isActive()).isTrue();
            assertThat(response.isFound()).isTrue();
        }

        @Test
        @DisplayName("Should create not found response correctly")
        void shouldCreateNotFoundResponseCorrectly() {
            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = 
                AnalyticsUseCase.UrlAnalyticsResponse.notFound();

            // Then
            assertThat(response.getShortCode()).isNull();
            assertThat(response.getOriginalUrl()).isNull();
            assertThat(response.getClickCount()).isZero();
            assertThat(response.getCreatedAt()).isNull();
            assertThat(response.getLastAccessedAt()).isNull();
            assertThat(response.getExpiresAt()).isNull();
            assertThat(response.isActive()).isFalse();
            assertThat(response.isFound()).isFalse();
        }

        @Test
        @DisplayName("Should create system analytics response correctly")
        void shouldCreateSystemAnalyticsResponseCorrectly() {
            // Given
            List<AnalyticsUseCase.UrlAnalyticsResponse> topUrls = Arrays.asList(
                AnalyticsUseCase.UrlAnalyticsResponse.fromUrlMapping(
                    new UrlMapping("top1", "https://example1.com", null)),
                AnalyticsUseCase.UrlAnalyticsResponse.fromUrlMapping(
                    new UrlMapping("top2", "https://example2.com", null))
            );

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = 
                new AnalyticsUseCase.SystemAnalyticsResponse(100, 500, 80, 20, topUrls);

            // Then
            assertThat(response.getTotalUrls()).isEqualTo(100);
            assertThat(response.getTotalClicks()).isEqualTo(500);
            assertThat(response.getActiveUrls()).isEqualTo(80);
            assertThat(response.getExpiredUrls()).isEqualTo(20);
            assertThat(response.getTopUrls()).hasSize(2);
            assertThat(response.getTopUrls().get(0).getShortCode()).isEqualTo("top1");
            assertThat(response.getTopUrls().get(1).getShortCode()).isEqualTo("top2");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should propagate repository exceptions in URL analytics")
        void shouldPropagateRepositoryExceptionsInUrlAnalytics() {
            // Given
            when(urlMappingRepository.findByShortCode(SHORT_CODE))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThatThrownBy(() -> analyticsUseCase.getUrlAnalytics(SHORT_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
            
            verify(urlMappingRepository).findByShortCode(SHORT_CODE);
        }

        @Test
        @DisplayName("Should propagate repository exceptions in system analytics")
        void shouldPropagateRepositoryExceptionsInSystemAnalytics() {
            // Given
            when(urlMappingRepository.findAll())
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThatThrownBy(() -> analyticsUseCase.getSystemAnalytics())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
            
            verify(urlMappingRepository).findAll();
        }

        @Test
        @DisplayName("Should handle null short code gracefully")
        void shouldHandleNullShortCodeGracefully() {
            // Given
            when(urlMappingRepository.findByShortCode(null)).thenReturn(Optional.empty());

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(null);

            // Then
            assertThat(response.isFound()).isFalse();
            verify(urlMappingRepository).findByShortCode(null);
        }
    }

    @Nested
    @DisplayName("Performance and Edge Case Tests")
    class PerformanceAndEdgeCaseTests {

        @Test
        @DisplayName("Should handle large dataset efficiently")
        void shouldHandleLargeDatasetEfficiently() {
            // Given - Create 1000 URLs with various click counts
            List<UrlMapping> mappings = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                mappings.add(new UrlMapping("code" + i, "https://example" + i + ".com", null));
            }
            
            when(urlMappingRepository.findAll()).thenReturn(mappings);

            // When
            AnalyticsUseCase.SystemAnalyticsResponse response = analyticsUseCase.getSystemAnalytics();

            // Then
            assertThat(response.getTotalUrls()).isEqualTo(1000);
            assertThat(response.getTotalClicks()).isEqualTo(0); // URLs start with 0 clicks
            assertThat(response.getTopUrls()).hasSize(10); // Limited to top 10
        }

        @Test
        @DisplayName("Should handle URLs with very high click counts")
        void shouldHandleUrlsWithVeryHighClickCounts() {
            // Given
            UrlMapping highClickMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(1), null, Integer.MAX_VALUE, LocalDateTime.now());
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(highClickMapping));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response.getClickCount()).isEqualTo(Integer.MAX_VALUE);
            assertThat(response.isFound()).isTrue();
        }

        @Test
        @DisplayName("Should handle edge case timestamps")
        void shouldHandleEdgeCaseTimestamps() {
            // Given
            LocalDateTime veryOldDate = LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime veryFutureDate = LocalDateTime.of(2099, 12, 31, 23, 59);
            
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                veryOldDate, veryFutureDate, 1, veryOldDate);
            
            when(urlMappingRepository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(mapping));

            // When
            AnalyticsUseCase.UrlAnalyticsResponse response = analyticsUseCase.getUrlAnalytics(SHORT_CODE);

            // Then
            assertThat(response.isFound()).isTrue();
            assertThat(response.getCreatedAt()).isEqualTo(veryOldDate.toString());
            assertThat(response.getExpiresAt()).isEqualTo(veryFutureDate.toString());
            assertThat(response.getLastAccessedAt()).isEqualTo(veryOldDate.toString());
        }
    }
}