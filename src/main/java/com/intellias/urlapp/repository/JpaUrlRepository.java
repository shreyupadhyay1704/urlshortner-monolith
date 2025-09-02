package com.intellias.urlapp.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intellias.urlapp.domain.UrlMapping;

@Repository
public interface JpaUrlRepository extends UrlRepository, JpaRepository<UrlMapping, String> {
    Optional<UrlMapping> findByFullUrl(String fullUrl);
}

