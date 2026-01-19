package com.chieaid24.insight_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.chieaid24.insight_service.dto.UsageDto;

@Component
public class UsageClient {
    private final RestTemplate restTemplate;

    private final String baseUrl;

    public UsageClient(@Value("${usage.service.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        String url = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path("/{userId}")
                    .queryParam("days", days)
                    .buildAndExpand(userId)
                    .toUriString();
        ResponseEntity<UsageDto> response = restTemplate.getForEntity(url, UsageDto.class);
        return response.getBody();
    }
}
