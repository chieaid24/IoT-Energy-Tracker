package com.chieaid24.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Builder;

@Builder
public record EnergyUsageDto(
    Long deviceId,
    double energyConsumed,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp) {}
