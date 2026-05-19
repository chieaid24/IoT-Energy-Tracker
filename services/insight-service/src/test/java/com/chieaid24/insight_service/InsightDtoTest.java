package com.chieaid24.insight_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chieaid24.insight_service.dto.AiInsightResponse;
import com.chieaid24.insight_service.dto.DeviceDto;
import com.chieaid24.insight_service.dto.InsightDto;
import com.chieaid24.insight_service.dto.UsageDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsightDtoTest {

  @Test
  void usageDtoCarriesUserAndDevices() {
    DeviceDto fridge =
        DeviceDto.builder()
            .id(1L)
            .name("Fridge")
            .type("REFRIGERATOR")
            .location("Kitchen")
            .energyConsumed(123.4)
            .build();
    UsageDto usage = UsageDto.builder().userId(42L).devices(List.of(fridge)).build();

    assertThat(usage.userId()).isEqualTo(42L);
    assertThat(usage.devices()).hasSize(1);
    assertThat(usage.devices().get(0).energyConsumed()).isEqualTo(123.4);
  }

  @Test
  void insightDtoBuildsExpectedShape() {
    InsightDto insight =
        InsightDto.builder()
            .userId(7L)
            .tips("Run dishwasher off-peak")
            .energyUsage(45.6)
            .confidence(88)
            .build();

    assertThat(insight.userId()).isEqualTo(7L);
    assertThat(insight.confidence()).isEqualTo(88);
    assertThat(insight.tips()).contains("dishwasher");
  }

  @Test
  void aiInsightResponseRecordExposesFields() {
    AiInsightResponse r = new AiInsightResponse(95, "Reduce HVAC usage");
    assertThat(r.confidence()).isEqualTo(95);
    assertThat(r.response()).isEqualTo("Reduce HVAC usage");
  }
}
