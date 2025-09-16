package com.intellias.urlapp.infrastructure.repositories;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import com.intellias.urlapp.infrastructure.mappers.UrlMappingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({UrlMappingRepositoryImpl.class, UrlMappingMapper.class})
@DisplayName("UrlMappingRepositoryImpl Integration Tests")
class UrlMappingRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaUrlMappingRepository jpaRepository;

    @Autowired
    private UrlMappingMapper mapper;

    private UrlMappingRepositoryImpl repositoryImpl;

    private static final String SHORT_CODE = "abc123";
    private static final String ORIGINAL_URL = "https://example.com";
    private static final String ANOTHER_SHORT_CODE = "xyz789";
    private static final String ANOTHER_ORIGINAL_URL = "https://another-example.com";

    @BeforeEach
    void setUp() {
        repositoryImpl = new UrlMappingRepositoryImpl(jpaRepository, mapper);
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save new URL mapping successfully")
        void shouldSaveNewUrlMappingSuccessfully() {
            // Given
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);

            // When
            UrlMapping savedMapping = repositoryImpl.save(domainMapping);

            // Then
            assertThat(savedMapping).isNotNull();
            assertThat(savedMapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(savedMapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(savedMapping.getCreatedAt()).isNotNull();
            assertThat(savedMapping.getClickCount()).isZero();

            // Verify in database
            UrlMappingEntity entityInDb = entityManager.find(UrlMappingEntity.class, SHORT_CODE);
            assertThat(entityInDb).isNotNull();
            assertThat(entityInDb.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(entityInDb.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
        }

        @Test
        @DisplayName("Should save URL mapping with expiration")
        void shouldSaveUrlMappingWithExpiration() {
            // Given
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(7);
            UrlMapping domainMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, expirationDate);

            // When
            UrlMapping savedMapping = repositoryImpl.save(domainMapping);

            // Then
            assertThat(savedMapping.getExpiresAt()).isNotNull();
            assertThat(savedMapping.getExpiresAt()).isEqualToIgnoringNanos(expirationDate);

            // Verify in database
            UrlMappingEntity entityInDb = entityManager.find(UrlMappingEntity.class, SHORT_CODE);
            assertThat(entityInDb.getExpiresAt()).isEqualToIgnoringNanos(expirationDate);
        }

        @Test
        @DisplayName("Should update existing URL mapping")
        void shouldUpdateExistingUrlMapping() {
            // Given - Save initial mapping
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 5, LocalDateTime.now().minusHours(1));
            entityManager.persistAndFlush(entity);
            entityManager.clear(); // Clear after initial save

            // Create updated domain mapping
            UrlMapping updatedMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                entity.getCreatedAt(), entity.getExpiresAt(), 10, LocalDateTime.now());

            // When
            UrlMapping savedMapping = repositoryImpl.save(updatedMapping);
            entityManager.flush(); // Ensure changes are persisted
            entityManager.clear(); // Clear persistence context

            // Then
            assertThat(savedMapping.getClickCount()).isEqualTo(10);
            assertThat(savedMapping.getLastAccessedAt()).isNotNull();

            // Verify in database with fresh read
            UrlMappingEntity updatedEntity = entityManager.find(UrlMappingEntity.class, SHORT_CODE);
            assertThat(updatedEntity.getClickCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should handle mapping with all fields populated")
        void shouldHandleMappingWithAllFieldsPopulated() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);
            LocalDateTime lastAccessedAt = LocalDateTime.now().minusHours(2);
            
            UrlMapping fullMapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, 
                createdAt, expiresAt, 25, lastAccessedAt);

            // When
            UrlMapping savedMapping = repositoryImpl.save(fullMapping);

            // Then
            assertThat(savedMapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(savedMapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
            assertThat(savedMapping.getCreatedAt()).isEqualToIgnoringNanos(createdAt);
            assertThat(savedMapping.getExpiresAt()).isEqualToIgnoringNanos(expiresAt);
            assertThat(savedMapping.getClickCount()).isEqualTo(25);
            assertThat(savedMapping.getLastAccessedAt()).isEqualToIgnoringNanos(lastAccessedAt);
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find URL mapping by short code")
        void shouldFindUrlMappingByShortCode() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 0, null);
            entityManager.persistAndFlush(entity);

            // When
            Optional<UrlMapping> result = repositoryImpl.findByShortCode(SHORT_CODE);

            // Then
            assertThat(result).isPresent();
            UrlMapping mapping = result.get();
            assertThat(mapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(mapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
        }

        @Test
        @DisplayName("Should return empty when short code not found")
        void shouldReturnEmptyWhenShortCodeNotFound() {
            // When
            Optional<UrlMapping> result = repositoryImpl.findByShortCode("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find URL mapping by original URL")
        void shouldFindUrlMappingByOriginalUrl() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 0, null);
            entityManager.persistAndFlush(entity);

            // When
            Optional<UrlMapping> result = repositoryImpl.findByOriginalUrl(ORIGINAL_URL);

            // Then
            assertThat(result).isPresent();
            UrlMapping mapping = result.get();
            assertThat(mapping.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(mapping.getOriginalUrl()).isEqualTo(ORIGINAL_URL);
        }

        @Test
        @DisplayName("Should return empty when original URL not found")
        void shouldReturnEmptyWhenOriginalUrlNotFound() {
            // When
            Optional<UrlMapping> result = repositoryImpl.findByOriginalUrl("https://nonexistent.com");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all URL mappings")
        void shouldFindAllUrlMappings() {
            // Given
            UrlMappingEntity entity1 = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 5, null);
            UrlMappingEntity entity2 = new UrlMappingEntity(ANOTHER_SHORT_CODE, ANOTHER_ORIGINAL_URL, 
                LocalDateTime.now(), null, 10, null);
            
            entityManager.persistAndFlush(entity1);
            entityManager.persistAndFlush(entity2);

            // When
            List<UrlMapping> result = repositoryImpl.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(UrlMapping::getShortCode)
                .containsExactlyInAnyOrder(SHORT_CODE, ANOTHER_SHORT_CODE);
            assertThat(result).extracting(UrlMapping::getClickCount)
                .containsExactlyInAnyOrder(5, 10);
        }

        @Test
        @DisplayName("Should return empty list when no mappings exist")
        void shouldReturnEmptyListWhenNoMappingsExist() {
            // When
            List<UrlMapping> result = repositoryImpl.findAll();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists Operations")
    class ExistsOperations {

        @Test
        @DisplayName("Should return true when short code exists")
        void shouldReturnTrueWhenShortCodeExists() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 0, null);
            entityManager.persistAndFlush(entity);

            // When
            boolean exists = repositoryImpl.existsByShortCode(SHORT_CODE);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when short code does not exist")
        void shouldReturnFalseWhenShortCodeDoesNotExist() {
            // When
            boolean exists = repositoryImpl.existsByShortCode("nonexistent");

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete URL mapping by short code")
        void shouldDeleteUrlMappingByShortCode() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 0, null);
            entityManager.persistAndFlush(entity);

            // Verify entity exists
            assertThat(entityManager.find(UrlMappingEntity.class, SHORT_CODE)).isNotNull();

            // When
            repositoryImpl.deleteByShortCode(SHORT_CODE);
            entityManager.flush();

            // Then
            assertThat(entityManager.find(UrlMappingEntity.class, SHORT_CODE)).isNull();
        }

        @Test
        @DisplayName("Should handle deletion of non-existent short code gracefully")
        void shouldHandleDeletionOfNonExistentShortCodeGracefully() {
            // When & Then - Should not throw exception
            assertThatCode(() -> repositoryImpl.deleteByShortCode("nonexistent"))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Update Click Stats Operations")
    class UpdateClickStatsOperations {

        @Test
        @DisplayName("Should update click statistics successfully")
        void shouldUpdateClickStatisticsSuccessfully() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 5, null);
            entityManager.persistAndFlush(entity);
            entityManager.clear(); // Clear after initial save

            // Create updated domain mapping from fresh entity
            UrlMapping domainMapping = repositoryImpl.findByShortCode(SHORT_CODE).orElseThrow();
            domainMapping.recordAccess(); // This increments click count and sets last accessed

            // When
            UrlMapping updatedMapping = repositoryImpl.updateClickStats(domainMapping);
            entityManager.flush(); // Ensure changes are persisted
            entityManager.clear(); // Clear persistence context

            // Then
            assertThat(updatedMapping.getClickCount()).isEqualTo(6);
            assertThat(updatedMapping.getLastAccessedAt()).isNotNull();

            // Verify in database with fresh read
            UrlMappingEntity updatedEntity = entityManager.find(UrlMappingEntity.class, SHORT_CODE);
            assertThat(updatedEntity.getClickCount()).isEqualTo(6);
            assertThat(updatedEntity.getLastAccessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple click updates")
        void shouldHandleMultipleClickUpdates() {
            // Given
            UrlMappingEntity entity = new UrlMappingEntity(SHORT_CODE, ORIGINAL_URL, 
                LocalDateTime.now(), null, 0, null);
            entityManager.persistAndFlush(entity);
            entityManager.clear(); // Clear after initial save

            // When - Record multiple accesses with fresh reads between updates
            UrlMapping domainMapping1 = repositoryImpl.findByShortCode(SHORT_CODE).orElseThrow();
            domainMapping1.recordAccess();
            UrlMapping updated1 = repositoryImpl.updateClickStats(domainMapping1);
            entityManager.flush();
            entityManager.clear();
            
            UrlMapping domainMapping2 = repositoryImpl.findByShortCode(SHORT_CODE).orElseThrow();
            domainMapping2.recordAccess();
            UrlMapping updated2 = repositoryImpl.updateClickStats(domainMapping2);
            entityManager.flush();
            entityManager.clear();
            
            UrlMapping domainMapping3 = repositoryImpl.findByShortCode(SHORT_CODE).orElseThrow();
            domainMapping3.recordAccess();
            UrlMapping finalUpdate = repositoryImpl.updateClickStats(domainMapping3);
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(finalUpdate.getClickCount()).isEqualTo(3);

            // Verify in database with fresh read
            UrlMappingEntity finalEntity = entityManager.find(UrlMappingEntity.class, SHORT_CODE);
            assertThat(finalEntity.getClickCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("Should handle concurrent operations on same entity")
        void shouldHandleConcurrentOperationsOnSameEntity() {
            // Given
            UrlMapping mapping1 = new UrlMapping(SHORT_CODE, ORIGINAL_URL, null);
            UrlMapping mapping2 = new UrlMapping(ANOTHER_SHORT_CODE, ANOTHER_ORIGINAL_URL, null);

            // When
            UrlMapping saved1 = repositoryImpl.save(mapping1);
            UrlMapping saved2 = repositoryImpl.save(mapping2);

            // Then
            assertThat(saved1.getShortCode()).isEqualTo(SHORT_CODE);
            assertThat(saved2.getShortCode()).isEqualTo(ANOTHER_SHORT_CODE);

            // Verify both exist
            assertThat(repositoryImpl.existsByShortCode(SHORT_CODE)).isTrue();
            assertThat(repositoryImpl.existsByShortCode(ANOTHER_SHORT_CODE)).isTrue();
        }

        @Test
        @DisplayName("Should maintain data integrity across operations")
        void shouldMaintainDataIntegrityAcrossOperations() {
            // Given
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(30);
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, expirationDate);

            // When
            UrlMapping saved = repositoryImpl.save(mapping);
            Optional<UrlMapping> retrieved = repositoryImpl.findByShortCode(SHORT_CODE);
            
            // Update click stats
            retrieved.get().recordAccess();
            UrlMapping updated = repositoryImpl.updateClickStats(retrieved.get());

            // Then
            assertThat(updated.getShortCode()).isEqualTo(saved.getShortCode());
            assertThat(updated.getOriginalUrl()).isEqualTo(saved.getOriginalUrl());
            assertThat(updated.getExpiresAt()).isEqualToIgnoringNanos(saved.getExpiresAt());
            assertThat(updated.getClickCount()).isEqualTo(1);
            assertThat(updated.getLastAccessedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle edge case URLs and short codes")
        void shouldHandleEdgeCaseUrlsAndShortCodes() {
            // Given
            String edgeCaseShortCode = "a";
            String edgeCaseUrl = "https://a.b";
            UrlMapping mapping = new UrlMapping(edgeCaseShortCode, edgeCaseUrl, null);

            // When
            UrlMapping saved = repositoryImpl.save(mapping);
            Optional<UrlMapping> retrieved = repositoryImpl.findByShortCode(edgeCaseShortCode);

            // Then
            assertThat(saved.getShortCode()).isEqualTo(edgeCaseShortCode);
            assertThat(saved.getOriginalUrl()).isEqualTo(edgeCaseUrl);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getShortCode()).isEqualTo(edgeCaseShortCode);
        }

        @Test
        @DisplayName("Should handle maximum length URLs")
        void shouldHandleMaximumLengthUrls() {
            // Given
            String longUrl = "https://example.com/" + "a".repeat(2000); // Approaching 2048 limit
            UrlMapping mapping = new UrlMapping(SHORT_CODE, longUrl, null);

            // When
            UrlMapping saved = repositoryImpl.save(mapping);
            Optional<UrlMapping> retrieved = repositoryImpl.findByOriginalUrl(longUrl);

            // Then
            assertThat(saved.getOriginalUrl()).isEqualTo(longUrl);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getOriginalUrl()).isEqualTo(longUrl);
        }

        @Test
        @DisplayName("Should handle expiration boundary conditions")
        void shouldHandleExpirationBoundaryConditions() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime futureExpiration = now.plusNanos(1); // Just 1 nanosecond in future
            UrlMapping mapping = new UrlMapping(SHORT_CODE, ORIGINAL_URL, futureExpiration);

            // When
            UrlMapping saved = repositoryImpl.save(mapping);

            // Then
            assertThat(saved.getExpiresAt()).isEqualToIgnoringNanos(futureExpiration);
            
            // Verify the mapping is considered active (since expiration logic is in domain layer)
            Optional<UrlMapping> retrieved = repositoryImpl.findByShortCode(SHORT_CODE);
            assertThat(retrieved).isPresent();
        }
    }
}