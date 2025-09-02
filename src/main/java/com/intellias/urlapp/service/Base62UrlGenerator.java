package com.intellias.urlapp.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.intellias.urlapp.util.AppConstants;

@Component
public class Base62UrlGenerator implements UrlGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateShortUrl() {
        StringBuilder sb = new StringBuilder(AppConstants.SHORT_URL_LENGTH);
        for (int i = 0; i < AppConstants.SHORT_URL_LENGTH; i++) {
            int idx = random.nextInt(AppConstants.BASE62_CHARSET.length());
            sb.append(AppConstants.BASE62_CHARSET.charAt(idx));
        }
        return sb.toString();
    }
}

