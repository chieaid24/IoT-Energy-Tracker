package com.chieaid24.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
  private Long id;
  private String name;
  private String surname;
  private String email;
  private String address;
  private boolean alerting;
  private double energyAlertingThreshold;
}
