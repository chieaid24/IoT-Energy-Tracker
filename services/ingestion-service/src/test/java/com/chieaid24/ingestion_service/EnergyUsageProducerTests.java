package com.chieaid24.ingestion_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chieaid24.ingestion_service.dto.EnergyUsageDto;
import com.chieaid24.ingestion_service.service.IngestionService;
import com.chieaid24.kafka.event.EnergyUsageEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"energy-usage"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class EnergyUsageProducerTests {

  // Stub the @Value-driven properties used by ParallelDataSimulator and DeviceClient so the
  // context boots. None of these are exercised by the test itself.
  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.kafka.producer.value-serializer",
        () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    r.add(
        "spring.kafka.producer.key-serializer",
        () -> "org.apache.kafka.common.serialization.StringSerializer");
    r.add("simulation.parallel-threads", () -> "1");
    r.add("simulation.requests-per-interval", () -> "1");
    r.add("simulation.interval-ms", () -> "60000");
    r.add("ingestion.endpoint", () -> "http://localhost/ingest");
    r.add("device.service.url", () -> "http://localhost/devices");
  }

  @Autowired private IngestionService ingestionService;
  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  @Test
  void publishesEnergyUsageEventAndRoundTripsThroughJson() {
    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EnergyUsageEvent.class.getName());
    consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    try (KafkaConsumer<String, EnergyUsageEvent> consumer = new KafkaConsumer<>(consumerProps)) {
      consumer.subscribe(List.of("energy-usage"));

      Instant ts = Instant.parse("2026-01-01T12:00:00Z");
      ingestionService.ingestEnergyUsage(new EnergyUsageDto(42L, 123.45, ts));

      ConsumerRecord<String, EnergyUsageEvent> record =
          KafkaTestUtils.getSingleRecord(consumer, "energy-usage", Duration.ofSeconds(15));

      assertThat(record.value().deviceId()).isEqualTo(42L);
      assertThat(record.value().energyConsumed()).isEqualTo(123.45);
      assertThat(record.value().timestamp()).isEqualTo(ts);
    }
  }
}
