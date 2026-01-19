package com.chieaid24.usage_service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.chieaid24.kafka.event.AlertingEvent;
import com.chieaid24.kafka.event.EnergyUsageEvent;
import com.chieaid24.usage_service.client.DeviceClient;
import com.chieaid24.usage_service.client.UserClient;
import com.chieaid24.usage_service.dto.DeviceDto;
import com.chieaid24.usage_service.dto.UserDto;
import com.chieaid24.usage_service.model.DeviceEnergy;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UsageService {

    private InfluxDBClient influxDBClient;
    private DeviceClient deviceClient;
    private UserClient userClient;

    @Value("${influx.bucket}")
    private String bucket;
    
    @Value("${influx.org}")
    private String influxOrg;

    private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;

    public UsageService(InfluxDBClient influxDBClient, DeviceClient deviceClient, UserClient userClient, KafkaTemplate<String, AlertingEvent> kafkaTemplate) {
        this.influxDBClient = influxDBClient;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "energy-usage", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
        // log.info("Received Energy Usage Event: {}", energyUsageEvent);
        Point point = Point.measurement("energy_usage")
            .addTag("deviceId", String.valueOf(energyUsageEvent.deviceId()))
            .addField("energyConsumed", energyUsageEvent.energyConsumed())
            .time(energyUsageEvent.timestamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(bucket, influxOrg, point);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void aggregateDeviceEnergyUsage() {
        final Instant now = Instant.now();
        final Instant oneHourAgo = now.minusSeconds(3600); // aggregate data from the last 10 minutes
        
        String fluxQuery = String.format("""
                from(bucket: "%s")
                |> range(start: time(v: "%s"), stop: time(v: "%s"))
                |> filter(fn: (r) => r["_measurement"] == "energy_usage")
                |> filter(fn: (r) => r["_field"] == "energyConsumed")
                |> group(columns: ["deviceId"])
                |> sum(column: "_value")
                """, bucket, oneHourAgo.toString(), now);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

        List<DeviceEnergy> deviceEnergies = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String deviceIdStr = (String) record.getValueByKey("deviceId");
                Double energyConsumed = record.getValueByKey("_value") instanceof Number ? 
                    ((Number) record.getValueByKey("_value")).doubleValue() : 0.0;

                deviceEnergies.add(
                    DeviceEnergy.builder()
                        .deviceId(Long.valueOf(deviceIdStr))
                        .energyConsumed(energyConsumed)
                        .build()
                );
            }
        }
        // log.info("Aggregated Device Energy Usage over the past hour: {}", deviceEnergies);
        
        for (DeviceEnergy deviceEnergy : deviceEnergies) {
            try {
            final DeviceDto deviceResponse = deviceClient.getDeviceById(deviceEnergy.getDeviceId());

            if (deviceResponse == null || deviceResponse.id() == null) {
                log.warn("Device not found for ID: {}", deviceEnergy.getDeviceId());
                continue;
            }
            deviceEnergy.setUserId(deviceResponse.userId());
            } catch (Exception e) {
            log.error("Error fetching device data for ID: {}", deviceEnergy.getDeviceId(), e);
            }
        }

        // remove devices with null userId
        deviceEnergies.removeIf(de -> de.getUserId() == null);

        // get user-device mapping ie map of userId to their list of DeviceEnergy
        Map<Long, List<DeviceEnergy>> userDeviceEnergyMap = deviceEnergies.stream()
            .collect(Collectors.groupingBy(DeviceEnergy::getUserId));

        // log.info("User Device Energy Map: {}", userDeviceEnergyMap);

        // get users energy consumption thresholds
        List<Long> userIds = new ArrayList<>(userDeviceEnergyMap.keySet());
        final Map<Long, Double> userThresholdMap = new HashMap<>();
        final Map<Long, String> userEmailMap = new HashMap<>();

        for (final Long userId : userIds) {
            try {
                UserDto user = userClient.getUserById(userId);
                if (user == null || user.id() == null || !user.alerting()) {
                    log.warn("User not found or alerting disabled for ID: {}", userId);
                    continue;
                }
                userThresholdMap.put(userId, user.energyAlertingThreshold());
                userEmailMap.put(userId, user.email());
            } catch (Exception e) {
                log.error("Error fetching user data for ID: {}", userId, e);
            }
        }
        log.info("User Threshold Map: {}", userThresholdMap);

        // check thresholds against aggregated usage
        final List<Long> alertedUsers = new ArrayList<>(userThresholdMap.keySet());
        for (final Long userId : alertedUsers) {
            final Double threshold = userThresholdMap.get(userId);
            final List<DeviceEnergy> devices = userDeviceEnergyMap.get(userId);

            final Double totalEnergyConsumed = devices.stream()
                .mapToDouble(DeviceEnergy::getEnergyConsumed)
                .sum();

            if (totalEnergyConsumed > threshold) {
                final String userEmail = userEmailMap.get(userId);
                log.warn("WARNING: User ID {} has exceeded the energy consumption threshold! Total Consumed: {}, Threshold: {}, Email: {}",
                    userId, totalEnergyConsumed, threshold, userEmail);
                // Put message on kafka alert topic
                final AlertingEvent alertingEvent = AlertingEvent.builder()
                    .userId(userId)
                    .message("Energy consumption exceeded threshold")
                    .threshold(threshold)
                    .energyConsumed(totalEnergyConsumed)
                    .email(userEmail)
                    .build();
                kafkaTemplate.send("energy-alerts", alertingEvent);
            } else {
                log.info("User ID {} is within the energy consumption threshold. Total Consumed: {}, Threshold: {}",
                    userId, totalEnergyConsumed, threshold);
            }

        }
    }
}