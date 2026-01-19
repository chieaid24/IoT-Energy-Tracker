package com.chieaid24.usage_service.dto;

public record UserDto(
        Long id,
        String name,
        String surname,
        String email,
        String address,
        Boolean alerting,
        Double energyAlertingThreshold
) {

}
