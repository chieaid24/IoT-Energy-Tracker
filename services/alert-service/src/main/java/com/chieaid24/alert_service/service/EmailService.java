package com.chieaid24.alert_service.service;

import com.chieaid24.alert_service.entity.Alert;
import com.chieaid24.alert_service.repository.AlertRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
  private final JavaMailSender mailSender;
  private final AlertRepository alertRepository;

  public EmailService(JavaMailSender mailSender, AlertRepository alertRepository) {
    this.mailSender = mailSender;
    this.alertRepository = alertRepository;
  }

  public void sendHtmlEmail(String to, String subject, String htmlBody, Long userId) {
    log.info("Sending email to: {}, subject: {}", to, subject);

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
      helper.setTo(to);
      helper.setFrom("noreply@chieaid24.com");
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(mimeMessage);

      final Alert alertSent =
          Alert.builder()
              .userId(userId)
              .sent(true)
              .createdAt(java.time.LocalDateTime.now())
              .build();
      alertRepository.saveAndFlush(alertSent);
    } catch (MailException | MessagingException e) {
      log.error("Failed to send email to: {}, error: {}", to, e.getMessage());
      final Alert alertFailed =
          Alert.builder()
              .userId(userId)
              .sent(false)
              .createdAt(java.time.LocalDateTime.now())
              .build();
      alertRepository.saveAndFlush(alertFailed);
    }

    log.info("Email sent to: {}", to);
  }
}
