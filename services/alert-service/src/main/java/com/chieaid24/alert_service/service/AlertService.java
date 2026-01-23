package com.chieaid24.alert_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.chieaid24.kafka.event.AlertingEvent;

@Slf4j
@Service
public class AlertService {

    private final EmailService emailService;

    public AlertService(EmailService emailService) {
        this.emailService = emailService;
    }

    // TODO: prettify the content, and add more info like device details, timestamp, etc
    // also handle failures and retries
    @KafkaListener(topics = "energy-alerts", groupId = "alert-service")
    public void energyUsageAlertEvent(AlertingEvent alertingEvent) {
        log.info("Received alerting event: {}", alertingEvent);
        // Process the alerting event (e.g., send notification, log to database, etc.)
        final String subject = "Energy Usage Alert for User " + alertingEvent.getUserId();
        final String message = "Alert: " + alertingEvent.getMessage() + "\n" +
                               "Threshold: " + alertingEvent.getThreshold() + "\n" +
                               "Energy Consumed: " + alertingEvent.getEnergyConsumed();
        emailService.sendEmail(alertingEvent.getEmail(), subject, message, alertingEvent.getUserId());
        
    }

}
