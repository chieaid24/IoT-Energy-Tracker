package com.chieaid24.ingestion_service.controller;

import com.chieaid24.ingestion_service.simulation.ParallelDataSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion/simulation")
public class SimulationController {

  private final ParallelDataSimulator simulator;

  public SimulationController(ParallelDataSimulator simulator) {
    this.simulator = simulator;
  }

  @PostMapping("/start")
  public ResponseEntity<Void> start() {
    boolean started = simulator.start();
    if (started) {
      return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @PostMapping("/stop")
  public ResponseEntity<Void> stop() {
    boolean stopped = simulator.stop();
    if (stopped) {
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @GetMapping("/status")
  public ResponseEntity<SimulationStatus> status() {
    return ResponseEntity.ok(new SimulationStatus(simulator.isRunning()));
  }

  public record SimulationStatus(boolean running) {}
}
