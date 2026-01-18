package com.chieaid24.usage_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.chieaid24.kafka.event.EnergyUsageEvent;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UsageService {

    private InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucket;
    
    @Value("${influx.org}")
    private String influxOrg;

    public UsageService(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @KafkaListener(topics = "energy-usage", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
        log.info("Received Energy Usage Event: {}", energyUsageEvent);
        Point point = Point.measurement("energy_usage")
            .addTag("deviceId", String.valueOf(energyUsageEvent.deviceId()))
            .addField("energyConsumed", energyUsageEvent.energyConsumed())
            .time(energyUsageEvent.timestamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(bucket, influxOrg, point);
    }
}
