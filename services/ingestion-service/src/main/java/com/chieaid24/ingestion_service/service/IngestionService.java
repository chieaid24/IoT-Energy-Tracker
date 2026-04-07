package com.chieaid24.ingestion_service.service;

import com.chieaid24.ingestion_service.dto.EnergyUsageDto;
import com.chieaid24.ingestion_service.dto.ShellyStatusDto;
import com.chieaid24.kafka.event.EnergyUsageEvent;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionService {
  private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

  public IngestionService(KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void ingestEnergyUsage(EnergyUsageDto input) {
    EnergyUsageEvent event =
        EnergyUsageEvent.builder()
            .deviceId(input.deviceId())
            .energyConsumed(input.energyConsumed())
            .timestamp(input.timestamp())
            .build();

    kafkaTemplate.send("energy-usage", event);
    // log.info("Ingested energy usage event: {}", event);
  }

  public void ingestShellyUsage(Long deviceId, ShellyStatusDto shellyStatus) {
    double power = shellyStatus.apower() != null ? shellyStatus.apower() : 0.0;

    EnergyUsageEvent event =
        EnergyUsageEvent.builder()
            .deviceId(deviceId)
            .energyConsumed(power)
            .timestamp(Instant.now())
            .build();

    kafkaTemplate
        .send("energy-usage", event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error(
                    "Failed to send Shelly event to Kafka for deviceId={}: {}",
                    deviceId,
                    ex.getMessage());
              }
            });
    log.info("Ingested Shelly energy usage event: {}", event);
  }
}
