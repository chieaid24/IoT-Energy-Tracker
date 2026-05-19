package com.chieaid24.device_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chieaid24.device_service.entity.Device;
import com.chieaid24.device_service.model.DeviceType;
import org.junit.jupiter.api.Test;

class DeviceTest {

  @Test
  void builderPopulatesAllFields() {
    Device device =
        Device.builder()
            .id(42L)
            .name("Kitchen Light")
            .type(DeviceType.LIGHT)
            .location("Kitchen")
            .userId(7L)
            .build();

    assertThat(device.getId()).isEqualTo(42L);
    assertThat(device.getName()).isEqualTo("Kitchen Light");
    assertThat(device.getType()).isEqualTo(DeviceType.LIGHT);
    assertThat(device.getLocation()).isEqualTo("Kitchen");
    assertThat(device.getUserId()).isEqualTo(7L);
  }

  @Test
  void deviceTypeEnumCoversExpectedAppliances() {
    assertThat(DeviceType.values())
        .contains(
            DeviceType.SPEAKER,
            DeviceType.THERMOSTAT,
            DeviceType.REFRIGERATOR,
            DeviceType.AIR_CONDITIONER,
            DeviceType.MISC);
  }
}
