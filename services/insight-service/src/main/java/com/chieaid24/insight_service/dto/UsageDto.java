package com.chieaid24.insight_service.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record UsageDto(Long userId, List<DeviceDto> devices) {}
