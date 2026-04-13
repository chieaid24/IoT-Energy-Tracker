package com.chieaid24.alert_service.controller;

import com.chieaid24.alert_service.entity.Alert;
import com.chieaid24.alert_service.repository.AlertRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/alert")
public class AlertController {

  private final AlertRepository alertRepository;

  public AlertController(AlertRepository alertRepository) {
    this.alertRepository = alertRepository;
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Alert>> getAlertsByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(alertRepository.findByUserIdOrderByCreatedAtDesc(userId));
  }

  @GetMapping("/user/{userId}/count")
  public ResponseEntity<Long> getAlertCountByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(alertRepository.countByUserId(userId));
  }
}
