package com.chieaid24.ingestion_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DeviceClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;

  public DeviceClient(@Value("${device.service.url}") String baseUrl) {
    this.restTemplate = new RestTemplate();
    this.baseUrl = baseUrl;
  }

  public Long getDeviceCount() {
    String url = UriComponentsBuilder.fromUriString(baseUrl).path("/total").toUriString();

    ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
    return response.getBody();
  }
}
