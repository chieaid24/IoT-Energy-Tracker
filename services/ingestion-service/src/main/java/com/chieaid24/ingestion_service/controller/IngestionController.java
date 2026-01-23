package com.chieaid24.ingestion_service.controller;

import com.chieaid24.ingestion_service.dto.EnergyUsageDto;
import com.chieaid24.ingestion_service.service.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

  // init vars and such
  private final IngestionService ingestionService;

  public IngestionController(IngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public void ingestData(@RequestBody EnergyUsageDto usageDto) {
    ingestionService.ingestEnergyUsage(usageDto);
  }
}
