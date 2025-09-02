package com.intellias.urlapp.controller;



import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellias.urlapp.domain.UrlRequest;
import com.intellias.urlapp.domain.UrlResponse;
import com.intellias.urlapp.service.UrlShortenerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/url")
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<UrlResponse> shorten(
            @Valid @RequestBody UrlRequest urlRequest,
            HttpServletRequest request) {
        String slug = service.shortener(urlRequest.getFullUrl()); // should return only the slug, e.g. 9MC3yUC
        String baseUrl =
            request.getScheme() + "://" +
            request.getServerName() + ":" +
            request.getServerPort();
        String shortUrl = baseUrl + "/" + slug;
        return ResponseEntity.ok(new UrlResponse(shortUrl));
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<String> redirect(@PathVariable String shortUrl) {
        String longUrl = service.fullURL(shortUrl);
        System.out.println("ResponseEntity.status(302).location(URI.create(longUrl)).build()   "+ResponseEntity.status(302).location(URI.create(longUrl)).build());
        return ResponseEntity.status(200).location(URI.create(longUrl)).build();
    }
}
