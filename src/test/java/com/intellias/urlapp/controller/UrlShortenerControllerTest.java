package com.intellias.urlapp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellias.urlapp.domain.UrlRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UrlShortenerControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private Environment environment;

    @Test
    void testShortenUrl() {
        // Prepare request
        UrlRequest request = new UrlRequest();
        request.setFullUrl("https://spring.io");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UrlRequest> entity = new HttpEntity<>(request, headers);

        // POST to shorten endpoint
        ResponseEntity<String> response = restTemplate.postForEntity("/api/url/shorten", entity, String.class);

        // Get the random port assigned at runtime
        String port = environment.getProperty("local.server.port");
        String expectedPrefix = "http://localhost:" + port + "/";

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains(expectedPrefix);
    }


    @Test
    void testShortenAndRedirect() throws Exception {
        // 1. Shorten a URL
        UrlRequest request = new UrlRequest();
        request.setFullUrl("https://spring.io");
        
        ResponseEntity<String> shortenResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/url/shorten", request, String.class);

        assertThat(shortenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
     //   assertThat(shortenResponse.getBody()).contains("http://localhost:" + port + "/");
        assertThat(shortenResponse.getBody()).contains("/api/url/");


        // Extract short URL from JSON response
        String shortUrl = objectMapper.readTree(shortenResponse.getBody()).get("shortUrl").asText();

        // 2. Redirect via short URL
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(shortUrl, Void.class);
        URI location = redirectResponse.getHeaders().getLocation();

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(location.toString()).isEqualTo("https://spring.io");
    }
}

