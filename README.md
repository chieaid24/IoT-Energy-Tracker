<h1 align="center">⚡️ Home IoT Energy Tracker</h1>

> Java Spring Boot microservice-based architecture to handle 500k+ users and 2.5m+ devices. Aggregates energy usage, analyzes + stores time-series IoT data, and produces AI-approved efficiency insights.

## System Design
<img width="6288" height="4516" alt="IoT Telemetry System Design4" src="https://github.com/user-attachments/assets/74640047-1b3c-47b9-98df-3fb663cf7181" />

## Tools / Frameworks Used
| Category | Tools |
| --- | --- |
| Language / Frameworks | Java 21 (Maven), Spring Boot, Spring Actuator |
| Database | MySQL, InfluxDB, Flyway |
| Microservices | Kafka, Ollama (deepseek-r1) |
| Dev Tools | Docker, Docker Compose, Mailpit |

## Technical Highlights
- Event-driven pipeline with **Kafka decoupling** ingestion, processing, and alerting.
- Dual persistence model: **MySQL** for relational data and **InfluxDB** for time-series usage analytics.
- AI-powered insights service that integrates **Ollama** (deepseek-r1) for actionable recommendations.
- **Multi-threaded** simulation in ingestion-service to stress test throughput and backpressure locally.
- **Health-aware services** via Spring Actuator endpoints for production-ready checks.

## General Service Overview

`user-service` (port 8080)
- Manages user profiles and identity data.
- Owns relational data and migrations for user records.

`device-service` (port 8081)
- Manages registered devices tied to users.
- Provides device metadata for usage attribution.

`ingestion-service` (port 8082)
- Receives usage events.
- Runs a multi-threaded event simulator during development.
- Publishes usage events to Kafka.

`usage-service` (port 8083)
- Consumes usage events from Kafka.
- Stores time-series data in InfluxDB.
- Enriches usage with user/device data.
- Emits alert events for high usage.

`alert-service` (port 8084)
- Consumes alert events from Kafka.
- Persists alerts in MySQL.
- Sends notifications through SMTP (Mailpit in local dev).

`insight-service` (port 8085)
- Aggregates usage data from usage-service.
- Calls Ollama to generate efficiency recommendations.

## High Level Data Flow
- `ingestion-service` publishes `energy-usage` events to Kafka.
- `usage-service` consumes events, writes to InfluxDB, and produces alert events.
- `alert-service` consumes alert events, stores alerts in MySQL, and sends email.
- `insight-service` runs on a cron schedule, requesting usage aggregates and calling Ollama for insights.

## Run With Docker

Pre-pull Ollama (recommended for slow networks):
```
docker pull ollama/ollama:latest
```

Build service images:
```
docker compose build
```

Start the stack:
```
docker compose up -d
```

Check status:
```
docker compose ps
```

Stop the stack:
```
docker compose down
```

## Health Checks
Each service exposes Spring Actuator health at:
- http://localhost:8080/actuator/health (user-service)
- http://localhost:8081/actuator/health (device-service)
- http://localhost:8082/actuator/health (ingestion-service)
- http://localhost:8083/actuator/health (usage-service)
- http://localhost:8084/actuator/health (alert-service)
- http://localhost:8085/actuator/health (insight-service)

## Project Layout
- `alert-service/`, `device-service/`, `ingestion-service/`, `insight-service/`, `usage-service/`, `user-service/`
- `docker/` contains init scripts and Kafka data volume mapping.
- `docker-compose.yml` defines the full local stack.

## Future Extensions
- Containerize and deploy the microservices to Kubernetes with HPA autoscaling, ingress, and centralized observability (maybe Prometheus/Grafana + logs/traces).
- Migrate cluster to AWS EKS with IAM, ALB/NLB ingress, and use more cloud-native, decoupled services (ex. convert `alerting-service` to a Lambda function, MSK instead of Kafka, etc)
