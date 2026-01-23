package com.chieaid24.usage_service.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record UsageDto(Long userId, List<DeviceDto> devices) {}
