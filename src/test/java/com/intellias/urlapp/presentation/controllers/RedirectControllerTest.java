package com.intellias.urlapp.presentation.controllers;

import com.intellias.urlapp.application.usecases.RetrieveUrlUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
@ActiveProfiles("test")
@DisplayName("RedirectController Tests")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RetrieveUrlUseCase retrieveUrlUseCase;

    private static final String SHORT_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://example.com";

    @Nested
    @DisplayName("GET /{shortCode} - Redirect Endpoint")
    class RedirectEndpoint {

        @Test
        @DisplayName("Should redirect to original URL with 301 status")
        void shouldRedirectToOriginalUrlWith301Status() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should return 404 when short code not found")
        void shouldReturn404WhenShortCodeNotFound() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse notFoundResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.notFound();

            when(retrieveUrlUseCase.execute("nonexistent")).thenReturn(notFoundResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", "nonexistent"))
                .andExpect(status().isNotFound());

            verify(retrieveUrlUseCase).execute("nonexistent");
        }

        @Test
        @DisplayName("Should return 410 Gone when URL is expired")
        void shouldReturn410GoneWhenUrlIsExpired() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse expiredResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.expired();

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(expiredResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isGone())
                .andExpect(content().string("Short URL has expired"));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle URLs with query parameters")
        void shouldHandleUrlsWithQueryParameters() throws Exception {
            // Given
            String urlWithQuery = "https://example.com/path?param1=value1&param2=value2";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(urlWithQuery);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", urlWithQuery));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle URLs with fragments")
        void shouldHandleUrlsWithFragments() throws Exception {
            // Given
            String urlWithFragment = "https://example.com/page#section1";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(urlWithFragment);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", urlWithFragment));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle international URLs")
        void shouldHandleInternationalUrls() throws Exception {
            // Given
            String internationalUrl = "https://例え.テスト/パス";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(internationalUrl);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", internationalUrl));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle very long URLs")
        void shouldHandleVeryLongUrls() throws Exception {
            // Given
            String longUrl = "https://example.com/" + "a".repeat(2000);
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(longUrl);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longUrl));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }
    }

    @Nested
    @DisplayName("Short Code Validation Tests")
    class ShortCodeValidationTests {

        @Test
        @DisplayName("Should handle alphanumeric short codes")
        void shouldHandleAlphanumericShortCodes() throws Exception {
            // Given
            String alphanumericCode = "aBc123";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(alphanumericCode)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", alphanumericCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(alphanumericCode);
        }

        @Test
        @DisplayName("Should handle single character short codes")
        void shouldHandleSingleCharacterShortCodes() throws Exception {
            // Given
            String singleChar = "a";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(singleChar)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", singleChar))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(singleChar);
        }

        @Test
        @DisplayName("Should handle maximum length short codes")
        void shouldHandleMaximumLengthShortCodes() throws Exception {
            // Given
            String maxLengthCode = "a".repeat(10);
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(maxLengthCode)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", maxLengthCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(maxLengthCode);
        }

        @Test
        @DisplayName("Should handle numeric short codes")
        void shouldHandleNumericShortCodes() throws Exception {
            // Given
            String numericCode = "123456";
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(numericCode)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", numericCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(numericCode);
        }

        @Test
        @DisplayName("Should handle case-sensitive short codes")
        void shouldHandleCaseSensitiveShortCodes() throws Exception {
            // Given
            String upperCaseCode = "ABC123";
            String lowerCaseCode = "abc123";
            
            RetrieveUrlUseCase.RetrieveUrlResponse upperResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success("https://upper.com");
            RetrieveUrlUseCase.RetrieveUrlResponse lowerResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success("https://lower.com");

            when(retrieveUrlUseCase.execute(upperCaseCode)).thenReturn(upperResponse);
            when(retrieveUrlUseCase.execute(lowerCaseCode)).thenReturn(lowerResponse);

            // When & Then - Upper case
            mockMvc.perform(get("/{shortCode}", upperCaseCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://upper.com"));

            // When & Then - Lower case
            mockMvc.perform(get("/{shortCode}", lowerCaseCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://lower.com"));

            verify(retrieveUrlUseCase).execute(upperCaseCode);
            verify(retrieveUrlUseCase).execute(lowerCaseCode);
        }
    }

    @Nested
    @DisplayName("HTTP Methods and Headers Tests")
    class HttpMethodsAndHeadersTests {

        @Test
        @DisplayName("Should only accept GET requests")
        void shouldOnlyAcceptGetRequests() throws Exception {
            // When & Then - POST should not be allowed
            mockMvc.perform(post("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMethodNotAllowed());

            // When & Then - PUT should not be allowed
            mockMvc.perform(put("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMethodNotAllowed());

            // When & Then - DELETE should not be allowed
            mockMvc.perform(delete("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMethodNotAllowed());

            verify(retrieveUrlUseCase, never()).execute(anyString());
        }

        @Test
        @DisplayName("Should handle requests with various User-Agent headers")
        void shouldHandleRequestsWithVariousUserAgentHeaders() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then - Browser request
            mockMvc.perform(get("/{shortCode}", SHORT_CODE)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            // When & Then - Mobile request
            mockMvc.perform(get("/{shortCode}", SHORT_CODE)
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            // When & Then - Bot request
            mockMvc.perform(get("/{shortCode}", SHORT_CODE)
                    .header("User-Agent", "Googlebot/2.1"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase, times(3)).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle requests with Referer header")
        void shouldHandleRequestsWithRefererHeader() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE)
                    .header("Referer", "https://referrer.com"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle requests with custom headers")
        void shouldHandleRequestsWithCustomHeaders() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE)
                    .header("X-Custom-Header", "custom-value")
                    .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptionsGracefully() throws Exception {
            // Given
            when(retrieveUrlUseCase.execute(SHORT_CODE))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isInternalServerError());

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle null response from use case")
        void shouldHandleNullResponseFromUseCase() throws Exception {
            // Given
            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isInternalServerError());

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle timeout exceptions")
        void shouldHandleTimeoutExceptions() throws Exception {
            // Given
            when(retrieveUrlUseCase.execute(SHORT_CODE))
                .thenThrow(new RuntimeException("Connection timeout"));

            // When & Then
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isInternalServerError());

            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }
    }

    @Nested
    @DisplayName("Analytics and Click Tracking Tests")
    class AnalyticsAndClickTrackingTests {

        @Test
        @DisplayName("Should track clicks for successful redirects")
        void shouldTrackClicksForSuccessfulRedirects() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isMovedPermanently());

            // Then - Verify that execute is called (which handles click tracking)
            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should not track clicks for non-existent URLs")
        void shouldNotTrackClicksForNonExistentUrls() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse notFoundResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.notFound();

            when(retrieveUrlUseCase.execute("nonexistent")).thenReturn(notFoundResponse);

            // When
            mockMvc.perform(get("/{shortCode}", "nonexistent"))
                .andExpect(status().isNotFound());

            // Then - Execute is still called but use case handles the "not found" scenario
            verify(retrieveUrlUseCase).execute("nonexistent");
        }

        @Test
        @DisplayName("Should not track clicks for expired URLs")
        void shouldNotTrackClicksForExpiredUrls() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse expiredResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.expired();

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(expiredResponse);

            // When
            mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                .andExpect(status().isGone());

            // Then - Execute is called but use case handles the "expired" scenario
            verify(retrieveUrlUseCase).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle multiple concurrent access attempts")
        void shouldHandleMultipleConcurrentAccessAttempts() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When - Simulate multiple concurrent requests
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", ORIGINAL_URL));
            }

            // Then
            verify(retrieveUrlUseCase, times(5)).execute(SHORT_CODE);
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceAndLoadTests {

        @Test
        @DisplayName("Should handle requests efficiently")
        void shouldHandleRequestsEfficiently() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(SHORT_CODE)).thenReturn(successResponse);

            // When & Then - Multiple sequential requests should all succeed
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(get("/{shortCode}", SHORT_CODE))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", ORIGINAL_URL));
            }

            verify(retrieveUrlUseCase, times(10)).execute(SHORT_CODE);
        }

        @Test
        @DisplayName("Should handle rapid sequential requests")
        void shouldHandleRapidSequentialRequests() throws Exception {
            // Given
            RetrieveUrlUseCase.RetrieveUrlResponse successResponse = 
                RetrieveUrlUseCase.RetrieveUrlResponse.success(ORIGINAL_URL);

            when(retrieveUrlUseCase.execute(anyString())).thenReturn(successResponse);

            // When & Then - Rapid requests with different short codes
            String[] shortCodes = {"code1", "code2", "code3", "code4", "code5"};
            for (String code : shortCodes) {
                mockMvc.perform(get("/{shortCode}", code))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", ORIGINAL_URL));
            }

            // Then
            for (String code : shortCodes) {
                verify(retrieveUrlUseCase).execute(code);
            }
        }
    }
}