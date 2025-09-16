package com.intellias.urlapp.infrastructure.mappers;

import com.intellias.urlapp.domain.entities.UrlMapping;
import com.intellias.urlapp.infrastructure.entities.UrlMappingEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between domain entities and infrastructure entities.
 * This isolates the domain layer from infrastructure concerns.
 */
@Component
public class UrlMappingMapper {
    
    /**
     * Converts domain entity to infrastructure entity
     */
    public UrlMappingEntity toEntity(UrlMapping domain) {
        return new UrlMappingEntity(
            domain.getShortCode(),
            domain.getOriginalUrl(),
            domain.getCreatedAt(),
            domain.getExpiresAt(),
            domain.getClickCount(),
            domain.getLastAccessedAt()
        );
    }
    
    /**
     * Converts infrastructure entity to domain entity
     */
    public UrlMapping toDomain(UrlMappingEntity entity) {
        return new UrlMapping(
            entity.getShortCode(),
            entity.getOriginalUrl(),
            entity.getCreatedAt(),
            entity.getExpiresAt(),
            entity.getClickCount(),
            entity.getLastAccessedAt()
        );
    }
}