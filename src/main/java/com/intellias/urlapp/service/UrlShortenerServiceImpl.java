
package com.intellias.urlapp.service;


import java.util.Optional;

import org.springframework.stereotype.Service;

import com.intellias.urlapp.domain.UrlMapping;
import com.intellias.urlapp.repository.UrlRepository;
import com.intellias.urlapp.util.AppConstants;

@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final UrlRepository repository;
    private final UrlGenerator generator;

    public UrlShortenerServiceImpl(UrlRepository repository, UrlGenerator generator) {
        this.repository = repository;
        this.generator = generator;
    }

    @Override
    public String shortener(String fullUrl) {
        Optional<UrlMapping> existing = repository.findByFullUrl(fullUrl);
        if (existing.isPresent()) {
            return AppConstants.SHORT_URL_PREFIX + existing.get().getShortUrl();
        }

        String shortCode = generator.generateShortUrl();
        repository.save(new UrlMapping(shortCode, fullUrl));
        return AppConstants.SHORT_URL_PREFIX + shortCode;
    }

    @Override
    public String fullURL(String shortUrl) {
        String slug = shortUrl.replace(AppConstants.SHORT_URL_PREFIX, "");
        return repository.findByShortUrl(slug)
                .map(UrlMapping::getFullUrl)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found or expired"));
    }
}

