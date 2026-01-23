package com.chieaid24.device_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UserClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;

  public UserClient(@Value("${user.service.url}") String baseUrl) {
    this.restTemplate = new RestTemplate();
    this.baseUrl = baseUrl;
  }

  public Integer getUserCount() {
    String url = UriComponentsBuilder.fromUriString(baseUrl).path("/total").toUriString();

    ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);
    return response.getBody();
  }
}
