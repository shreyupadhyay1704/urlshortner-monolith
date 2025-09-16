package com.intellias.urlapp.configuration;

import com.intellias.urlapp.domain.repositories.UrlMappingRepository;
import com.intellias.urlapp.domain.services.UrlShorteningService;
import com.intellias.urlapp.domain.services.impl.UrlShorteningServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for dependency injection.
 * This wires up the Onion architecture layers properly.
 */
@Configuration
public class ApplicationConfiguration {
    
    /**
     * Creates the domain service bean with its repository dependency
     */
    @Bean
    public UrlShorteningService urlShorteningService(UrlMappingRepository urlMappingRepository) {
        return new UrlShorteningServiceImpl(urlMappingRepository);
    }
}