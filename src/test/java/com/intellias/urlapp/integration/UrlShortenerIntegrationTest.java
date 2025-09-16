package com.intellias.urlapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellias.urlapp.application.usecases.AnalyticsUseCase;
import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import com.intellias.urlapp.infrastructure.repositories.JpaUrlMappingRepository;
import com.intellias.urlapp.presentation.dto.ShortenUrlRequestDto;
import com.intellias.urlapp.presentation.dto.ShortenUrlResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("URL Shortener Integration Tests")
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUrlMappingRepository jpaRepository;

    private static final String ORIGINAL_URL = "https://example.com";
    private static final String ANOTHER_URL = "https://another-example.com";

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("Complete URL Shortening Workflow")
    class CompleteUrlShorteningWorkflow {

        @Test
        @DisplayName("Should complete entire workflow: shorten → redirect → analytics")
        void shouldCompleteEntireWorkflow() throws Exception {
            // Given
            ShortenUrlRequestDto shortenRequest = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);

            // Step 1: Shorten URL
            MvcResult shortenResult = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shortenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.originalUrl", is(ORIGINAL_URL)))
                .andExpect(jsonPath("$.message", is("URL shortened successfully")))
                .andReturn();

            // Extract short code from response
            ShortenUrlResponseDto shortenResponse = objectMapper.readValue(
                shortenResult.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
            String shortCode = shortenResponse.getShortCode();

            // Step 2: Verify URL exists in database
            assertThat(jpaRepository.existsByShortCode(shortCode)).isTrue();
            UrlMappingEntity entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(entity.getClickCount()).isZero();

            // Step 3: Redirect (access the shortened URL)
            mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            // Step 4: Verify click count was incremented in database
            entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isEqualTo(1);
            assertThat(entity.getLastAccessedAt()).isNotNull();

            // Step 5: Check analytics
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(shortCode)))
                .andExpect(jsonPath("$.originalUrl", is(ORIGINAL_URL)))
                .andExpect(jsonPath("$.clickCount", is(1)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.found", is(true)));

            // Step 6: Access multiple times and verify cumulative analytics
            mockMvc.perform(get("/{shortCode}", shortCode));
            mockMvc.perform(get("/{shortCode}", shortCode));

            mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", is(3)));
        }

        @Test
        @DisplayName("Should handle custom short code workflow")
        void shouldHandleCustomShortCodeWorkflow() throws Exception {
            // Given
            String customShortCode = "custom123";
            ShortenUrlRequestDto shortenRequest = new ShortenUrlRequestDto(ORIGINAL_URL, customShortCode, null);

            // Step 1: Shorten URL with custom code
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shortenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(customShortCode)))
                .andExpect(jsonPath("$.originalUrl", is(ORIGINAL_URL)));

            // Step 2: Verify custom short code works for redirection
            mockMvc.perform(get("/{shortCode}", customShortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            // Step 3: Verify analytics shows custom code
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", customShortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(customShortCode)))
                .andExpect(jsonPath("$.clickCount", is(1)));

            // Step 4: Try to create another URL with same custom code (should fail)
            ShortenUrlRequestDto duplicateRequest = new ShortenUrlRequestDto(ANOTHER_URL, customShortCode, null);
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already in use")));
        }

        @Test
        @DisplayName("Should handle URL expiration workflow")
        void shouldHandleUrlExpirationWorkflow() throws Exception {
            // Given - Create URL that expires immediately
            String shortCode = "expiring";
            LocalDateTime pastExpiration = LocalDateTime.now().minusHours(1);
            
            UrlMappingEntity expiredEntity = new UrlMappingEntity(
                shortCode, ORIGINAL_URL, LocalDateTime.now().minusDays(1), 
                pastExpiration, 0, null);
            jpaRepository.save(expiredEntity);

            // Step 1: Try to access expired URL
            mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isGone())
                .andExpect(content().string("Short URL has expired"));

            // Step 2: Verify click count wasn't incremented for expired URL
            UrlMappingEntity entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isZero();

            // Step 3: Verify analytics still shows the expired URL
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(shortCode)))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.clickCount", is(0)));
        }

        @Test
        @DisplayName("Should handle URL with expiration date workflow")
        void shouldHandleUrlWithExpirationDateWorkflow() throws Exception {
            // Given
            Integer expirationDays = 7;
            ShortenUrlRequestDto shortenRequest = new ShortenUrlRequestDto(ORIGINAL_URL, null, expirationDays);

            // Step 1: Create URL with expiration
            MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shortenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

            ShortenUrlResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
            String shortCode = response.getShortCode();

            // Step 2: Verify URL is active and can be accessed
            mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", ORIGINAL_URL));

            // Step 3: Verify analytics shows expiration date
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.clickCount", is(1)));
        }
    }

    @Nested
    @DisplayName("System Analytics Integration Tests")
    class SystemAnalyticsIntegrationTests {

        @Test
        @DisplayName("Should provide accurate system analytics")
        void shouldProvideAccurateSystemAnalytics() throws Exception {
            // Given - Create multiple URLs with different click counts
            createUrlAndAccess("url1", ORIGINAL_URL, 5);
            createUrlAndAccess("url2", ANOTHER_URL, 3);
            createUrlAndAccess("url3", "https://third-example.com", 7);

            // Create an expired URL
            UrlMappingEntity expiredEntity = new UrlMappingEntity(
                "expired", "https://expired.com", LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1), 2, LocalDateTime.now().minusDays(2));
            jpaRepository.save(expiredEntity);

            // When & Then - Check system analytics
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(4)))
                .andExpect(jsonPath("$.totalClicks", is(17))) // 5+3+7+2
                .andExpect(jsonPath("$.activeUrls", is(3)))
                .andExpect(jsonPath("$.expiredUrls", is(1)))
                .andExpect(jsonPath("$.topUrls", hasSize(4)))
                .andExpect(jsonPath("$.topUrls[0].clickCount", is(7))) // url3 should be first
                .andExpect(jsonPath("$.topUrls[1].clickCount", is(5))) // url1 should be second
                .andExpect(jsonPath("$.topUrls[2].clickCount", is(3))); // url2 should be third
        }

        @Test
        @DisplayName("Should handle empty system analytics")
        void shouldHandleEmptySystemAnalytics() throws Exception {
            // When & Then - No URLs in system
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(0)))
                .andExpect(jsonPath("$.totalClicks", is(0)))
                .andExpect(jsonPath("$.activeUrls", is(0)))
                .andExpect(jsonPath("$.expiredUrls", is(0)))
                .andExpect(jsonPath("$.topUrls", hasSize(0)));
        }

        private void createUrlAndAccess(String shortCode, String originalUrl, int accessCount) throws Exception {
            // Create URL mapping
            UrlMappingEntity entity = new UrlMappingEntity(
                shortCode, originalUrl, LocalDateTime.now(), null, 0, null);
            jpaRepository.save(entity);

            // Access the URL multiple times
            for (int i = 0; i < accessCount; i++) {
                mockMvc.perform(get("/{shortCode}", shortCode))
                    .andExpect(status().isMovedPermanently());
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Integration Tests")
    class ErrorHandlingIntegrationTests {

        @Test
        @DisplayName("Should handle non-existent URL gracefully")
        void shouldHandleNonExistentUrlGracefully() throws Exception {
            // When & Then - Access non-existent short code
            mockMvc.perform(get("/{shortCode}", "nonexistent"))
                .andExpect(status().isNotFound());

            // When & Then - Get analytics for non-existent short code
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", "nonexistent"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle invalid URL creation requests")
        void shouldHandleInvalidUrlCreationRequests() throws Exception {
            // Test missing URL
            ShortenUrlRequestDto invalidRequest1 = new ShortenUrlRequestDto(null, null, null);
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest1)))
                .andExpect(status().isBadRequest());

            // Test invalid URL format
            ShortenUrlRequestDto invalidRequest2 = new ShortenUrlRequestDto("invalid-url", null, null);
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest2)))
                .andExpect(status().isBadRequest());

            // Test negative expiration days
            ShortenUrlRequestDto invalidRequest3 = new ShortenUrlRequestDto(ORIGINAL_URL, null, -5);
            mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest3)))
                .andExpect(status().isBadRequest());

            // Verify no URLs were created
            assertThat(jpaRepository.count()).isZero();
        }

        @Test
        @DisplayName("Should handle existing URL reuse correctly")
        void shouldHandleExistingUrlReuseCorrectly() throws Exception {
            // Given - Create initial URL
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            
            MvcResult result1 = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            ShortenUrlResponseDto response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), ShortenUrlResponseDto.class);

            // When - Try to shorten the same URL again
            MvcResult result2 = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            ShortenUrlResponseDto response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), ShortenUrlResponseDto.class);

            // Then - Should return same short code
            assertThat(response2.getShortCode()).isEqualTo(response1.getShortCode());
            assertThat(jpaRepository.count()).isEqualTo(1); // Only one URL should exist
        }
    }

    @Nested
    @DisplayName("Database Persistence Integration Tests")
    class DatabasePersistenceIntegrationTests {

        @Test
        @DisplayName("Should persist URL mappings correctly across requests")
        void shouldPersistUrlMappingsCorrectlyAcrossRequests() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, 30);

            // Step 1: Create URL
            MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            ShortenUrlResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
            String shortCode = response.getShortCode();

            // Step 2: Verify persistence immediately
            UrlMappingEntity entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getShortCode()).isEqualTo(shortCode);
            assertThat(entity.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(entity.getClickCount()).isZero();
            assertThat(entity.getExpiresAt()).isNotNull();

            // Step 3: Access URL and verify persistence of click tracking
            mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isMovedPermanently());

            entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isEqualTo(1);
            assertThat(entity.getLastAccessedAt()).isNotNull();

            // Step 4: Access multiple times and verify cumulative persistence
            mockMvc.perform(get("/{shortCode}", shortCode));
            mockMvc.perform(get("/{shortCode}", shortCode));

            entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle concurrent access correctly")
        void shouldHandleConcurrentAccessCorrectly() throws Exception {
            // Given
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            
            MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            ShortenUrlResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
            String shortCode = response.getShortCode();

            // When - Simulate concurrent access
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/{shortCode}", shortCode))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", ORIGINAL_URL));
            }

            // Then - Verify all accesses were counted
            UrlMappingEntity entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should maintain data integrity across operations")
        void shouldMaintainDataIntegrityAcrossOperations() throws Exception {
            // Create multiple URLs
            String[] urls = {
                "https://example1.com",
                "https://example2.com", 
                "https://example3.com"
            };

            String[] shortCodes = new String[3];

            // Create all URLs
            for (int i = 0; i < urls.length; i++) {
                ShortenUrlRequestDto request = new ShortenUrlRequestDto(urls[i], null, null);
                MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

                ShortenUrlResponseDto response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
                shortCodes[i] = response.getShortCode();
            }

            // Access each URL different number of times
            for (int i = 0; i < shortCodes.length; i++) {
                for (int j = 0; j <= i; j++) {
                    mockMvc.perform(get("/{shortCode}", shortCodes[i]))
                        .andExpect(status().isMovedPermanently())
                        .andExpect(header().string("Location", urls[i]));
                }
            }

            // Verify data integrity
            assertThat(jpaRepository.count()).isEqualTo(3);

            for (int i = 0; i < shortCodes.length; i++) {
                UrlMappingEntity entity = jpaRepository.findByShortCode(shortCodes[i]).orElseThrow();
                assertThat(entity.getOriginalUrl()).isEqualTo(urls[i]);
                assertThat(entity.getClickCount()).isEqualTo(i + 1);
            }

            // Verify system analytics
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(3)))
                .andExpect(jsonPath("$.totalClicks", is(6))) // 1+2+3
                .andExpect(jsonPath("$.activeUrls", is(3)))
                .andExpect(jsonPath("$.expiredUrls", is(0)));
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    class PerformanceAndStressTests {

        @Test
        @DisplayName("Should handle high volume URL creation efficiently")
        void shouldHandleHighVolumeUrlCreationEfficiently() throws Exception {
            // When - Create multiple URLs
            int urlCount = 50;
            for (int i = 0; i < urlCount; i++) {
                ShortenUrlRequestDto request = new ShortenUrlRequestDto(
                    "https://example" + i + ".com", null, null);
                
                mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortCode").exists());
            }

            // Then - Verify all URLs were created
            assertThat(jpaRepository.count()).isEqualTo(urlCount);

            // Verify system analytics
            mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls", is(urlCount)))
                .andExpect(jsonPath("$.activeUrls", is(urlCount)));
        }

        @Test
        @DisplayName("Should handle high volume access efficiently")
        void shouldHandleHighVolumeAccessEfficiently() throws Exception {
            // Given - Create one URL
            ShortenUrlRequestDto request = new ShortenUrlRequestDto(ORIGINAL_URL, null, null);
            MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            ShortenUrlResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ShortenUrlResponseDto.class);
            String shortCode = response.getShortCode();

            // When - Access it many times
            int accessCount = 100;
            for (int i = 0; i < accessCount; i++) {
                mockMvc.perform(get("/{shortCode}", shortCode))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", ORIGINAL_URL));
            }

            // Then - Verify all accesses were counted
            UrlMappingEntity entity = jpaRepository.findByShortCode(shortCode).orElseThrow();
            assertThat(entity.getClickCount()).isEqualTo(accessCount);

            // Verify analytics
            mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", is(accessCount)));
        }
    }
}