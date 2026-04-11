package com.chieaid24.ingestion_service.service;

import com.chieaid24.ingestion_service.dto.EnergyUsageDto;
import com.chieaid24.ingestion_service.dto.ShellyStatusDto;
import com.chieaid24.kafka.event.EnergyUsageEvent;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionService {
  private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;
  private final ConcurrentHashMap<Long, Double> lastEnergyTotalByDevice = new ConcurrentHashMap<>();

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
    if (shellyStatus.aenergy() == null || shellyStatus.aenergy().total() == null) {
      log.warn("No aenergy data in Shelly status for deviceId={}, skipping", deviceId);
      return;
    }

    double currentTotal = shellyStatus.aenergy().total();
    Double previousTotal = lastEnergyTotalByDevice.put(deviceId, currentTotal);

    if (previousTotal == null) {
      log.info(
          "First reading for deviceId={}, baseline aenergy.total={} Wh. Skipping until next reading.",
          deviceId,
          currentTotal);
      return;
    }

    double energyDelta = currentTotal - previousTotal;
    if (energyDelta < 0) {
      log.warn(
          "aenergy.total decreased for deviceId={} ({}→{}), device may have reset. Re-baselining.",
          deviceId,
          previousTotal,
          currentTotal);
      return;
    }

    if (energyDelta == 0.0) {
      return;
    }

    EnergyUsageEvent event =
        EnergyUsageEvent.builder()
            .deviceId(deviceId)
            .energyConsumed(energyDelta)
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
    log.info("Ingested Shelly energy usage event: {} Wh", event);
  }
}
