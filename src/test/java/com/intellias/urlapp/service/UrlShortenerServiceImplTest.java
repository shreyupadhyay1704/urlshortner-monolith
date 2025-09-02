package com.intellias.urlapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.intellias.urlapp.domain.UrlMapping;
import com.intellias.urlapp.repository.UrlRepository;
import com.intellias.urlapp.util.AppConstants;

class UrlShortenerServiceImplTest {

    private UrlRepository repository;
    private UrlGenerator generator;
    private UrlShortenerServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UrlRepository.class);
        generator = Mockito.mock(UrlGenerator.class);
        service = new UrlShortenerServiceImpl(repository, generator);
    }

    @Test
    void testShortenerReturnsExistingUrl() {
        UrlMapping mapping = new UrlMapping("abc123", "https://google.com");
        when(repository.findByFullUrl("https://google.com")).thenReturn(Optional.of(mapping));

        String result = service.shortener("https://google.com");

        assertEquals(AppConstants.SHORT_URL_PREFIX + "abc123", result);
        verify(repository, never()).save(any());
    }

    @Test
    void testShortenerCreatesNewMapping() {
        when(repository.findByFullUrl("https://facebook.com")).thenReturn(Optional.empty());
        when(generator.generateShortUrl()).thenReturn("xyz789");

        String result = service.shortener("https://facebook.com");

        assertEquals(AppConstants.SHORT_URL_PREFIX + "xyz789", result);
        verify(repository).save(any(UrlMapping.class));
    }

    @Test
    void testFullUrlFound() {
        UrlMapping mapping = new UrlMapping("abc123", "https://google.com");
        when(repository.findByShortUrl("abc123")).thenReturn(Optional.of(mapping));

        String result = service.fullURL(AppConstants.SHORT_URL_PREFIX + "abc123");

        assertEquals("https://google.com", result);
    }

    @Test
    void testFullUrlNotFoundThrowsException() {
        when(repository.findByShortUrl("notfound")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.fullURL(AppConstants.SHORT_URL_PREFIX + "notfound"));
    }
}

