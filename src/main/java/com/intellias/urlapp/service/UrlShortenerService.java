package com.intellias.urlapp.service;

public interface UrlShortenerService {
    String shortener(String fullUrl);
    String fullURL(String shortUrl);
}
