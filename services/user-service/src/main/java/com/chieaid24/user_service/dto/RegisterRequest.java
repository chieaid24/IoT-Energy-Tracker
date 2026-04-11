package com.chieaid24.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
  private String name;
  private String surname;
  private String email;
  private String password;
  private Boolean alerting;
  private Double energyAlertingThreshold;
}
