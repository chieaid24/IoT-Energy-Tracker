package com.chieaid24.ingestion_service.simulation;

import com.chieaid24.ingestion_service.client.DeviceClient;
import com.chieaid24.ingestion_service.dto.EnergyUsageDto;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ParallelDataSimulator implements CommandLineRunner {

  private final RestTemplate restTemplate = new RestTemplate();
  private final Random random = new Random();
  private DeviceClient deviceClient;

  @Value("${simulation.parallel-threads}")
  private int parallelThreads;

  @Value("${simulation.requests-per-interval}")
  private int requestsPerInterval;

  @Value("${simulation.interval-ms}")
  private long intervalMs;

  @Value("${ingestion.endpoint}")
  private String ingestionEndpoint;

  private final ExecutorService executorService;
  private final ScheduledExecutorService scheduler;
  private final Object scheduleLock = new Object();
  private ScheduledFuture<?> scheduledTask;

  public ParallelDataSimulator(DeviceClient deviceClient) {
    this.deviceClient = deviceClient;
    this.executorService = Executors.newCachedThreadPool();
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Initializing simulator...");
    ((ThreadPoolExecutor) executorService).setCorePoolSize(parallelThreads);
  }

  public void sendMockData() {
    int batchSize = requestsPerInterval / parallelThreads;
    int remainder = requestsPerInterval % parallelThreads;

    for (int i = 0; i < parallelThreads; i++) {
      int requestsForThread = batchSize + (i < remainder ? 1 : 0);
      executorService.submit(
          () -> {
            for (int j = 0; j < requestsForThread; j++) {
              EnergyUsageDto dto =
                  EnergyUsageDto.builder()
                      .deviceId(random.nextLong(1, deviceClient.getDeviceCount() + 1))
                      .energyConsumed(Math.round(random.nextDouble(0.0, 2.0) * 100.0) / 100.0)
                      .timestamp(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
                      .build();
              try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<EnergyUsageDto> request = new HttpEntity<>(dto, headers);
                restTemplate.postForEntity(ingestionEndpoint, request, Void.class);

                // log.info("Sent mock data: {}", dto);
              } catch (Exception e) {
                log.error("Failed to send mock data: {}", dto, e);
              }
            }
          });
    }
    log.info(
        "Dispatched {} mock data requests across {} threads.",
        requestsPerInterval,
        parallelThreads);
  }

  public boolean start() {
    log.info("Attempting to START the Parallel Data Simulator...");
    synchronized (scheduleLock) {
      if (scheduledTask != null && !scheduledTask.isCancelled() && !scheduledTask.isDone()) {
        log.info("Parallel Data Simulator is already running.");
        return false;
      }
      scheduledTask =
          scheduler.scheduleAtFixedRate(this::sendMockData, 0, intervalMs, TimeUnit.MILLISECONDS);
      log.info("Started Parallel Data Simulator!");
      return true;
    }
  }

  public boolean stop() {
    log.info("Attempting to stop Parallel Data Simulator...");
    synchronized (scheduleLock) {
      if (scheduledTask == null) {
        log.info("Parallel Data Simulator is already stopped.");
        return false;
      }
      scheduledTask.cancel(false);
      scheduledTask = null;
      log.info("Stopped Parallel Data Simulator!");
      return true;
    }
  }

  public boolean isRunning() {
    log.info("Checking the status of Parallel Data Simulator...");
    synchronized (scheduleLock) {
      return scheduledTask != null && !scheduledTask.isCancelled() && !scheduledTask.isDone();
    }
  }

  @PreDestroy
  public void shutdown() {
    stop();
    scheduler.shutdown();
    executorService.shutdown();
    log.info("Shut down Parallel Data Simulator executor service.");
  }
}
