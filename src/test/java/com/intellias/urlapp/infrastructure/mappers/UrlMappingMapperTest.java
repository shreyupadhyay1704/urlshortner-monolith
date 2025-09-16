package com.intellias.urlapp.infrastructure.mappers;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UrlMappingMapper Tests")
class UrlMappingMapperTest {

    private UrlMappingMapper mapper;

    private static final String SHORT_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        mapper = new UrlMappingMapper();
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    class DomainToEntityMapping {

        @Test
        @DisplayName("Should convert domain entity to infrastructure entity with all fields")
        void shouldConvertDomainEntityToInfrastructureEntityWithAllFields() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);
            LocalDateTime lastAccessedAt = LocalDateTime.now().minusHours(2);
            int clickCount = 25;

            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, clickCount, lastAccessedAt);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(entity.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(entity.getClickCount()).isEqualTo(clickCount);
            assertThat(entity.getLastAccessedAt()).isEqualTo(lastAccessedAt);
        }

        @Test
        @DisplayName("Should convert domain entity with minimal fields")
        void shouldConvertDomainEntityWithMinimalFields() {
            // Given
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(entity.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(entity.getCreatedAt()).isNotNull(); // Set by domain constructor
            assertThat(entity.getExpiresAt()).isNull();
            assertThat(entity.getClickCount()).isZero();
            assertThat(entity.getLastAccessedAt()).isNull();
        }

        @Test
        @DisplayName("Should preserve null expiration date")
        void shouldPreserveNullExpirationDate() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, null, 0, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity.getExpiresAt()).isNull();
        }

        @Test
        @DisplayName("Should preserve null last accessed date")
        void shouldPreserveNullLastAccessedDate() {
            // Given
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity.getLastAccessedAt()).isNull();
        }

        @Test
        @DisplayName("Should handle zero click count")
        void shouldHandleZeroClickCount() {
            // Given
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity.getClickCount()).isZero();
        }

        @Test
        @DisplayName("Should handle high click count")
        void shouldHandleHighClickCount() {
            // Given
            int highClickCount = Integer.MAX_VALUE;
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, highClickCount, LocalDateTime.now());

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);

            // Then
            assertThat(entity.getClickCount()).isEqualTo(highClickCount);
        }
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    class EntityToDomainMapping {

        @Test
        @DisplayName("Should convert infrastructure entity to domain entity with all fields")
        void shouldConvertInfrastructureEntityToDomainEntityWithAllFields() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(3);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            LocalDateTime lastAccessedAt = LocalDateTime.now().minusMinutes(30);
            int clickCount = 42;

            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, clickCount, lastAccessedAt);

            // When
            UrlMapping domainMapping = mapper.toDomain(entity);

            // Then
            assertThat(domainMapping).isNotNull();
            assertThat(domainMapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(domainMapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(domainMapping.getCreatedAt()).isEqualTo(createdAt);
            assertThat(domainMapping.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(domainMapping.getClickCount()).isEqualTo(clickCount);
            assertThat(domainMapping.getLastAccessedAt()).isEqualTo(lastAccessedAt);
        }

        @Test
        @DisplayName("Should convert infrastructure entity with minimal fields")
        void shouldConvertInfrastructureEntityWithMinimalFields() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                createdAt, null, 0, null);

            // When
            UrlMapping domainMapping = mapper.toDomain(entity);

            // Then
            assertThat(domainMapping).isNotNull();
            assertThat(domainMapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(domainMapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(domainMapping.getCreatedAt()).isEqualTo(createdAt);
            assertThat(domainMapping.getExpiresAt()).isNull();
            assertThat(domainMapping.getClickCount()).isZero();
            assertThat(domainMapping.getLastAccessedAt()).isNull();
        }

        @Test
        @DisplayName("Should preserve business logic behavior after mapping")
        void shouldPreserveBusinessLogicBehaviorAfterMapping() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(1); // Future expiration
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 0, null);

            // When
            UrlMapping domainMapping = mapper.toDomain(entity);

            // Then
            assertThat(domainMapping.isActive()).isTrue();
            assertThat(domainMapping.isExpired()).isFalse();
            
            // Test business logic still works
            domainMapping.recordAccess();
            assertThat(domainMapping.getClickCount()).isEqualTo(1);
            assertThat(domainMapping.getLastAccessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle expired URL correctly")
        void shouldHandleExpiredUrlCorrectly() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
            LocalDateTime expiresAt = LocalDateTime.now().minusDays(1); // Past expiration
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 5, LocalDateTime.now().minusDays(2));

            // When
            UrlMapping domainMapping = mapper.toDomain(entity);

            // Then
            assertThat(domainMapping.isExpired()).isTrue();
            assertThat(domainMapping.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle entity with default JPA constructor")
        void shouldHandleEntityWithDefaultJpaConstructor() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity();
            entity.setShortCode(SHORT_CODE);
            entity.setOriginalUrl(ORIGINAL_URL);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setClickCount(0);

            // When
            UrlMapping domainMapping = mapper.toDomain(entity);

            // Then
            assertThat(domainMapping).isNotNull();
            assertThat(domainMapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(domainMapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(domainMapping.getCreatedAt()).isNotNull();
            assertThat(domainMapping.getClickCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping")
    class BidirectionalMapping {

        @Test
        @DisplayName("Should maintain data integrity in round-trip conversion")
        void shouldMaintainDataIntegrityInRoundTripConversion() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);
            LocalDateTime lastAccessedAt = LocalDateTime.now().minusHours(4);
            int clickCount = 15;

            UrlMapping originalDomain = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, clickCount, lastAccessedAt);

            // When - Domain -> Entity -> Domain
            UrlMappingEntity entity = mapper.toEntity(originalDomain);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(convertedDomain.getShortCode()).isEqualTo(originalDomain.getShortCode());
            assertThat(convertedDomain.getOriginalUrl()).isEqualTo(originalDomain.getOriginalUrl());
            assertThat(convertedDomain.getCreatedAt()).isEqualTo(originalDomain.getCreatedAt());
            assertThat(convertedDomain.getExpiresAt()).isEqualTo(originalDomain.getExpiresAt());
            assertThat(convertedDomain.getClickCount()).isEqualTo(originalDomain.getClickCount());
            assertThat(convertedDomain.getLastAccessedAt()).isEqualTo(originalDomain.getLastAccessedAt());
        }

        @Test
        @DisplayName("Should maintain equality after round-trip conversion")
        void shouldMaintainEqualityAfterRoundTripConversion() {
            // Given
            UrlMapping originalDomain = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When - Domain -> Entity -> Domain
            UrlMappingEntity entity = mapper.toEntity(originalDomain);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(convertedDomain).isEqualTo(originalDomain);
            assertThat(convertedDomain.hashCode()).isEqualTo(originalDomain.hashCode());
        }

        @Test
        @DisplayName("Should handle reverse conversion Entity -> Domain -> Entity")
        void shouldHandleReverseConversionEntityToDomainToEntity() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusHours(6);
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(18);
            UrlMappingEntity originalEntity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 8, null);

            // When - Entity -> Domain -> Entity
            UrlMapping domain = mapper.toDomain(originalEntity);
            UrlMappingEntity convertedEntity = mapper.toEntity(domain);

            // Then
            assertThat(convertedEntity.getShortCode()).isEqualTo(originalEntity.getShortCode());
            assertThat(convertedEntity.getOriginalUrl()).isEqualTo(originalEntity.getOriginalUrl());
            assertThat(convertedEntity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
            assertThat(convertedEntity.getExpiresAt()).isEqualTo(originalEntity.getExpiresAt());
            assertThat(convertedEntity.getClickCount()).isEqualTo(originalEntity.getClickCount());
            assertThat(convertedEntity.getLastAccessedAt()).isEqualTo(originalEntity.getLastAccessedAt());
        }

        @Test
        @DisplayName("Should handle multiple conversions without data loss")
        void shouldHandleMultipleConversionsWithoutDataLoss() {
            // Given
            UrlMapping originalDomain = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30), 100, LocalDateTime.now());

            // When - Multiple round trips
            UrlMappingEntity entity1 = mapper.toEntity(originalDomain);
            UrlMapping domain1 = mapper.toDomain(entity1);
            UrlMappingEntity entity2 = mapper.toEntity(domain1);
            UrlMapping domain2 = mapper.toDomain(entity2);

            // Then
            assertThat(domain2.getShortCode()).isEqualTo(originalDomain.getShortCode());
            assertThat(domain2.getOriginalUrl()).isEqualTo(originalDomain.getOriginalUrl());
            assertThat(domain2.getClickCount()).isEqualTo(originalDomain.getClickCount());
            assertThat(domain2.getCreatedAt()).isEqualTo(originalDomain.getCreatedAt());
            assertThat(domain2.getExpiresAt()).isEqualTo(originalDomain.getExpiresAt());
            assertThat(domain2.getLastAccessedAt()).isEqualTo(originalDomain.getLastAccessedAt());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle maximum length short code")
        void shouldHandleMaximumLengthShortCode() {
            // Given
            String maxLengthShortCode = "a".repeat(10); // Maximum allowed length
            UrlMapping domainMapping = new UrlMapping(maxLengthShortCode, ORIGINAL_URL, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getShortCode()).isEqualTo(maxLengthShortCode);
            assertThat(convertedDomain.getShortCode()).isEqualTo(maxLengthShortCode);
        }

        @Test
        @DisplayName("Should handle very long URLs")
        void shouldHandleVeryLongUrls() {
            // Given
            String longUrl = "https://example.com/" + "path/".repeat(400); // Very long URL
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, longUrl, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getOriginalUrl()).isEqualTo(longUrl);
            assertThat(convertedDomain.getOriginalUrl()).isEqualTo(longUrl);
        }

        @Test
        @DisplayName("Should handle edge case timestamps")
        void shouldHandleEdgeCaseTimestamps() {
            // Given
            LocalDateTime minDateTime = LocalDateTime.MIN;
            LocalDateTime maxDateTime = LocalDateTime.MAX;
            
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                minDateTime, maxDateTime, 0, minDateTime);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getCreatedAt()).isEqualTo(minDateTime);
            assertThat(entity.getExpiresAt()).isEqualTo(maxDateTime);
            assertThat(convertedDomain.getCreatedAt()).isEqualTo(minDateTime);
            assertThat(convertedDomain.getExpiresAt()).isEqualTo(maxDateTime);
        }

        @Test
        @DisplayName("Should handle maximum click count")
        void shouldHandleMaximumClickCount() {
            // Given
            int maxClickCount = Integer.MAX_VALUE;
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, maxClickCount, LocalDateTime.now());

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getClickCount()).isEqualTo(maxClickCount);
            assertThat(convertedDomain.getClickCount()).isEqualTo(maxClickCount);
        }

        @Test
        @DisplayName("Should handle URLs with special characters")
        void shouldHandleUrlsWithSpecialCharacters() {
            // Given
            String specialCharUrl = "https://example.com/path?query=value&other=测试#fragment";
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, specialCharUrl, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getOriginalUrl()).isEqualTo(specialCharUrl);
            assertThat(convertedDomain.getOriginalUrl()).isEqualTo(specialCharUrl);
        }

        @Test
        @DisplayName("Should handle short codes with mixed case and numbers")
        void shouldHandleShortCodesWithMixedCaseAndNumbers() {
            // Given
            String mixedCaseShortCode = "AbC123";
            UrlMapping domainMapping = new UrlMapping(mixedCaseShortCode, ORIGINAL_URL, null);

            // When
            UrlMappingEntity entity = mapper.toEntity(domainMapping);
            UrlMapping convertedDomain = mapper.toDomain(entity);

            // Then
            assertThat(entity.getShortCode()).isEqualTo(mixedCaseShortCode);
            assertThat(convertedDomain.getShortCode()).isEqualTo(mixedCaseShortCode);
        }
    }
}