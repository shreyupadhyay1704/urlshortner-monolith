package com.intellias.urlapp.repository;


import java.util.Optional;

import com.intellias.urlapp.domain.UrlMapping;

public interface UrlRepository {
    Optional<UrlMapping> findByShortUrl(String shortUrl);
    Optional<UrlMapping> findByFullUrl(String fullUrl);
    UrlMapping save(UrlMapping mapping);
}

