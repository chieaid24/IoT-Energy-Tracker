package com.chieaid24.insight_service.controller;

import com.chieaid24.insight_service.dto.InsightDto;
import com.chieaid24.insight_service.service.InsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insight")
public class InsightController {
  private final InsightService insightService;

  private InsightController(InsightService insightService) {
    this.insightService = insightService;
  }

  @GetMapping("/saving-tips/{userId}")
  public ResponseEntity<InsightDto> getSavingTips(@PathVariable Long userId) {
    InsightDto insightDto = insightService.getSavingTips(userId);
    return ResponseEntity.ok(insightDto);
  }

  // get usage data from last 3 days and AI powered insights
  @GetMapping("/overview/{userId}")
  public ResponseEntity<InsightDto> getOverview(@PathVariable Long userId) {
    InsightDto insightDto = insightService.getOverview(userId);
    return ResponseEntity.ok(insightDto);
  }
}
