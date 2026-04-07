package com.chieaid24.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellyStatusDto(
    Double apower, Double voltage, Double current, AEnergy aenergy, Temperature temperature) {

  @Builder
  public record AEnergy(
      Double total,
      @JsonProperty("by_minute") List<Double> byMinute,
      @JsonProperty("minute_ts") Long minuteTs) {}

  @Builder
  public record Temperature(Double tC, Double tF) {}
}
