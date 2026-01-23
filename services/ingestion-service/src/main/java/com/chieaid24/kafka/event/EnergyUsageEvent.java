package com.chieaid24.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Builder;

@Builder
public record EnergyUsageEvent(
    Long deviceId,
    double energyConsumed,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp) {}
