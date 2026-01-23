package com.chieaid24.usage_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;

@Configuration
public class InfluxDBConfig {

    @Value("${influx.url}")
    private String influxUrl;

    @Value("${influx.token}")
    private String influxToken;
    
    @Value("${influx.org}")
    private String influxOrg;

    @Bean
    public InfluxDBClient influxDBClient() {
        // Configure and return your InfluxDB client here
        return InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrg);
    }
}
