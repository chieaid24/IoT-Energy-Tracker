package com.chieaid24.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellyStatusDto(
    int id, boolean output, double apower, double voltage, double freq, double current) {}
