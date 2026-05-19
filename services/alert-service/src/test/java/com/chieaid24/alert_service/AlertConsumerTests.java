package com.chieaid24.alert_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chieaid24.alert_service.repository.AlertRepository;
import com.chieaid24.kafka.event.AlertingEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    topics = {"energy-alerts"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class AlertConsumerTests {

  @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  // DataSourceConfig reads spring.datasource.* and spring.datasource.replica.* directly, so
  // both pools must be pointed at the same container.
  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", mysql::getJdbcUrl);
    r.add("spring.datasource.username", mysql::getUsername);
    r.add("spring.datasource.password", mysql::getPassword);
    r.add("spring.datasource.replica.url", mysql::getJdbcUrl);
    r.add("spring.datasource.replica.username", mysql::getUsername);
    r.add("spring.datasource.replica.password", mysql::getPassword);
    // No Flyway in alert-service — let Hibernate create the alert table.
    r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    // Mirror production kafka serdes.
    r.add(
        "spring.kafka.producer.value-serializer",
        () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    r.add(
        "spring.kafka.producer.key-serializer",
        () -> "org.apache.kafka.common.serialization.StringSerializer");
    r.add(
        "spring.kafka.consumer.value-deserializer",
        () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
    r.add(
        "spring.kafka.consumer.key-deserializer",
        () -> "org.apache.kafka.common.serialization.StringDeserializer");
    r.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    r.add(
        "spring.kafka.consumer.properties.spring.json.value.default.type",
        () -> "com.chieaid24.kafka.event.AlertingEvent");
    r.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
    r.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
  }

  @MockitoBean private JavaMailSender mailSender;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private AlertRepository alertRepository;

  @BeforeEach
  void stubMimeMessage() {
    // EmailService builds a MimeMessageHelper around whatever createMimeMessage() returns;
    // hand back a real (but disconnected) MimeMessage so the helper can populate it.
    when(mailSender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
  }

  @Test
  void consumesAlertEventPersistsAlertRowAndInvokesMailSender() {
    AlertingEvent event =
        AlertingEvent.builder()
            .userId(99L)
            .name("Test User")
            .email("test@example.com")
            .message("threshold exceeded")
            .threshold(100.0)
            .energyConsumed(150.0)
            .build();

    kafkaTemplate.send("energy-alerts", event);

    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              assertThat(alertRepository.findAll())
                  .hasSize(1)
                  .first()
                  .satisfies(
                      a -> {
                        assertThat(a.getUserId()).isEqualTo(99L);
                        assertThat(a.isSent()).isTrue();
                      });
            });
    verify(mailSender).send(any(MimeMessage.class));
  }
}
