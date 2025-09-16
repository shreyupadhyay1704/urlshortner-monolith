package com.intellias.urlapp.domain.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UrlMapping Entity Tests")
class UrlMappingTest {

    private static final String VALID_SHORT_CODE = "abc123";
    private static final String VALID_ORIGINAL_URL = "https://example.com";
    private static final LocalDateTime FUTURE_DATE = LocalDateTime.now().plusDays(7);

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create UrlMapping with valid parameters")
        void shouldCreateUrlMappingWithValidParameters() {
            // When
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);

            // Then
            assertThat(urlMapping.getShortCode()).isEqualTo(VALID_SHORT_CODE);
            assertThat(urlMapping.getOriginalUrl()).isEqualTo(VALID_ORIGINAL_URL);
            assertThat(urlMapping.getExpiresAt()).isEqualTo(FUTURE_DATE);
            assertThat(urlMapping.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(urlMapping.getClickCount()).isZero();
            assertThat(urlMapping.getLastAccessedAt()).isNull();
        }

        @Test
        @DisplayName("Should create UrlMapping with all parameters")
        void shouldCreateUrlMappingWithAllParameters() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime lastAccessed = LocalDateTime.now().minusHours(1);
            int clickCount = 5;

            // When
            UrlMapping urlMapping = new UrlMapping(
                VALID_SHORT_CODE, 
                VALID_ORIGINAL_URL, 
                createdAt, 
                FUTURE_DATE, 
                clickCount, 
                lastAccessed
            );

            // Then
            assertThat(urlMapping.getShortCode()).isEqualTo(VALID_SHORT_CODE);
            assertThat(urlMapping.getOriginalUrl()).isEqualTo(VALID_ORIGINAL_URL);
            assertThat(urlMapping.getCreatedAt()).isEqualTo(createdAt);
            assertThat(urlMapping.getExpiresAt()).isEqualTo(FUTURE_DATE);
            assertThat(urlMapping.getClickCount()).isEqualTo(clickCount);
            assertThat(urlMapping.getLastAccessedAt()).isEqualTo(lastAccessed);
        }

        @Test
        @DisplayName("Should create UrlMapping without expiration")
        void shouldCreateUrlMappingWithoutExpiration() {
            // When
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, null);

            // Then
            assertThat(urlMapping.getExpiresAt()).isNull();
            assertThat(urlMapping.isActive()).isTrue();
            assertThat(urlMapping.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Short Code Validation Tests")
    class ShortCodeValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null or empty short codes")
        void shouldRejectNullOrEmptyShortCodes(String invalidShortCode) {
            // When & Then
            assertThatThrownBy(() -> new UrlMapping(invalidShortCode, VALID_ORIGINAL_URL, FUTURE_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Short code cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject short codes exceeding maximum length")
        void shouldRejectTooLongShortCodes() {
            // Given
            String tooLongShortCode = "a".repeat(11); // More than 10 characters

            // When & Then
            assertThatThrownBy(() -> new UrlMapping(tooLongShortCode, VALID_ORIGINAL_URL, FUTURE_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Short code cannot exceed 10 characters");
        }

        @Test
        @DisplayName("Should trim whitespace from short code")
        void shouldTrimWhitespaceFromShortCode() {
            // Given
            String shortCodeWithWhitespace = "  abc123  ";

            // When
            UrlMapping urlMapping = new UrlMapping(shortCodeWithWhitespace, VALID_ORIGINAL_URL, FUTURE_DATE);

            // Then
            assertThat(urlMapping.getShortCode()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("Should accept maximum length short code")
        void shouldAcceptMaximumLengthShortCode() {
            // Given
            String maxLengthShortCode = "a".repeat(10);

            // When
            UrlMapping urlMapping = new UrlMapping(maxLengthShortCode, VALID_ORIGINAL_URL, FUTURE_DATE);

            // Then
            assertThat(urlMapping.getShortCode()).isEqualTo(maxLengthShortCode);
        }
    }

    @Nested
    @DisplayName("Original URL Validation Tests")
    class OriginalUrlValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null or empty URLs")
        void shouldRejectNullOrEmptyUrls(String invalidUrl) {
            // When & Then
            assertThatThrownBy(() -> new UrlMapping(VALID_SHORT_CODE, invalidUrl, FUTURE_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Original URL cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"example.com", "ftp://example.com", "file://test.txt", "invalid-url"})
        @DisplayName("Should reject URLs without http or https protocol")
        void shouldRejectInvalidProtocols(String invalidUrl) {
            // When & Then
            assertThatThrownBy(() -> new UrlMapping(VALID_SHORT_CODE, invalidUrl, FUTURE_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL must start with http:// or https://");
        }

        @ParameterizedTest
        @ValueSource(strings = {"http://example.com", "https://example.com", "https://subdomain.example.com/path?query=value"})
        @DisplayName("Should accept valid HTTP and HTTPS URLs")
        void shouldAcceptValidUrls(String validUrl) {
            // When
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, validUrl, FUTURE_DATE);

            // Then
            assertThat(urlMapping.getOriginalUrl()).isEqualTo(validUrl);
        }

        @Test
        @DisplayName("Should trim whitespace from URL")
        void shouldTrimWhitespaceFromUrl() {
            // Given
            String urlWithWhitespace = "  https://example.com  ";

            // When
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, urlWithWhitespace, FUTURE_DATE);

            // Then
            assertThat(urlMapping.getOriginalUrl()).isEqualTo("https://example.com");
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should record access correctly")
        void shouldRecordAccessCorrectly() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);
            LocalDateTime beforeAccess = LocalDateTime.now();

            // When
            urlMapping.recordAccess();

            // Then
            assertThat(urlMapping.getClickCount()).isEqualTo(1);
            assertThat(urlMapping.getLastAccessedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeAccess)
                .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should increment click count on multiple accesses")
        void shouldIncrementClickCountOnMultipleAccesses() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);

            // When
            urlMapping.recordAccess();
            urlMapping.recordAccess();
            urlMapping.recordAccess();

            // Then
            assertThat(urlMapping.getClickCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should update last accessed time on each access")
        void shouldUpdateLastAccessedTimeOnEachAccess() throws InterruptedException {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);
            
            // When
            urlMapping.recordAccess();
            LocalDateTime firstAccess = urlMapping.getLastAccessedAt();
            
            Thread.sleep(1); // Ensure time difference
            urlMapping.recordAccess();
            LocalDateTime secondAccess = urlMapping.getLastAccessedAt();

            // Then
            assertThat(secondAccess).isAfter(firstAccess);
        }
    }

    @Nested
    @DisplayName("Expiration Tests")
    class ExpirationTests {

        @Test
        @DisplayName("Should not be expired when expiration is in future")
        void shouldNotBeExpiredWhenExpirationIsInFuture() {
            // Given
            LocalDateTime futureExpiration = LocalDateTime.now().plusDays(1);
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, futureExpiration);

            // When & Then
            assertThat(urlMapping.isExpired()).isFalse();
            assertThat(urlMapping.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should be expired when expiration is in past")
        void shouldBeExpiredWhenExpirationIsInPast() {
            // Given
            LocalDateTime pastExpiration = LocalDateTime.now().minusDays(1);
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, pastExpiration);

            // When & Then
            assertThat(urlMapping.isExpired()).isTrue();
            assertThat(urlMapping.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should never expire when expiration is null")
        void shouldNeverExpireWhenExpirationIsNull() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, null);

            // When & Then
            assertThat(urlMapping.isExpired()).isFalse();
            assertThat(urlMapping.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should be expired exactly at expiration time")
        void shouldBeExpiredExactlyAtExpirationTime() {
            // Given
            LocalDateTime exactExpiration = LocalDateTime.now().minusNanos(1);
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, exactExpiration);

            // When & Then
            assertThat(urlMapping.isExpired()).isTrue();
            assertThat(urlMapping.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when short codes are the same")
        void shouldBeEqualWhenShortCodesAreTheSame() {
            // Given
            UrlMapping urlMapping1 = new UrlMapping("same", "https://example1.com", FUTURE_DATE);
            UrlMapping urlMapping2 = new UrlMapping("same", "https://example2.com", null);

            // When & Then
            assertThat(urlMapping1).isEqualTo(urlMapping2);
            assertThat(urlMapping1.hashCode()).isEqualTo(urlMapping2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when short codes are different")
        void shouldNotBeEqualWhenShortCodesAreDifferent() {
            // Given
            UrlMapping urlMapping1 = new UrlMapping("code1", VALID_ORIGINAL_URL, FUTURE_DATE);
            UrlMapping urlMapping2 = new UrlMapping("code2", VALID_ORIGINAL_URL, FUTURE_DATE);

            // When & Then
            assertThat(urlMapping1).isNotEqualTo(urlMapping2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);

            // When & Then
            assertThat(urlMapping).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);

            // When & Then
            assertThat(urlMapping).isEqualTo(urlMapping);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain essential information in toString")
        void shouldContainEssentialInformationInToString() {
            // Given
            UrlMapping urlMapping = new UrlMapping(VALID_SHORT_CODE, VALID_ORIGINAL_URL, FUTURE_DATE);
            urlMapping.recordAccess();

            // When
            String toString = urlMapping.toString();

            // Then
            assertThat(toString)
                .contains("UrlMapping")
                .contains(VALID_SHORT_CODE)
                .contains(VALID_ORIGINAL_URL)
                .contains("clickCount=1");
        }
    }
}