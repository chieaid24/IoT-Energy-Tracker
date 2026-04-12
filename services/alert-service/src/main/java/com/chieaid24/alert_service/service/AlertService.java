package com.chieaid24.alert_service.service;

import com.chieaid24.kafka.event.AlertingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertService {

  private final EmailService emailService;
  private final EcoEmailBuilder ecoEmailBuilder;

  public AlertService(EmailService emailService, EcoEmailBuilder ecoEmailBuilder) {
    this.emailService = emailService;
    this.ecoEmailBuilder = ecoEmailBuilder;
  }

  @KafkaListener(topics = "energy-alerts", groupId = "alert-service")
  public void energyUsageAlertEvent(AlertingEvent alertingEvent) {
    log.info("Received alerting event: {}", alertingEvent);

    final String subject = "Energy Usage Alert for " + alertingEvent.getName();
    final String htmlBody =
        ecoEmailBuilder.buildHtmlEmail(
            alertingEvent.getMessage(),
            alertingEvent.getThreshold(),
            alertingEvent.getEnergyConsumed());

    emailService.sendHtmlEmail(
        alertingEvent.getEmail(), subject, htmlBody, alertingEvent.getUserId());
  }
}
