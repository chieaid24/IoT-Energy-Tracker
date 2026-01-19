# Energy Tracker

Energy Tracker is a microservice-based backend for collecting device energy usage, storing time-series data, generating alerts, and producing AI-powered efficiency insights. It is designed for local development with Docker Compose and can scale to distributed deployments.

Use cases include:
- Tracking household or lab device energy usage over time.
- Detecting anomalous or high-usage behavior and generating alerts.
- Simulating device usage streams to test pipelines.
- Producing personalized efficiency recommendations via an AI model.

| Tool | Purpose |
| --- | --- |
| Java 21 | Service runtime |
| Spring Boot | Web, data, and integration framework |
| Maven | Build and dependency management |
| MySQL | Relational persistence (users, devices, alerts) |
| Kafka | Event streaming between services |
| InfluxDB | Time-series usage storage |
| Ollama | Local LLM runtime for insights |
| Mailpit | Local SMTP sink for alerts |
| Docker + Docker Compose | Containerized development |
| Flyway | DB migrations (user-service) |
| Spring Actuator | Health endpoints |

## Services and Responsibilities

user-service (port 8080)
- Manages user profiles and identity data.
- Owns relational data and migrations for user records.

device-service (port 8081)
- Manages registered devices tied to users.
- Provides device metadata for usage attribution.

ingestion-service (port 8082)
- Simulates or receives usage events.
- Publishes usage events to Kafka.

usage-service (port 8083)
- Consumes usage events from Kafka.
- Stores time-series data in InfluxDB.
- Enriches usage with user/device data.
- Emits alert events for high usage.

alert-service (port 8084)
- Consumes alert events from Kafka.
- Persists alerts in MySQL.
- Sends notifications through SMTP (Mailpit in local dev).

insight-service (port 8085)
- Aggregates usage data from usage-service.
- Calls Ollama to generate efficiency recommendations.

## Data Flow (High Level)
- ingestion-service publishes energy-usage events to Kafka.
- usage-service consumes events, writes to InfluxDB, and produces alert events.
- alert-service consumes alert events, stores alerts in MySQL, and sends email.
- insight-service requests usage aggregates and calls Ollama for insights.

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

## Configuration
Docker Compose wires internal service URLs and infrastructure endpoints. Key variables include:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `INFLUX_URL`, `INFLUX_TOKEN`, `INFLUX_ORG`, `INFLUX_BUCKET`
- `DEVICE_SERVICE_URL`, `USER_SERVICE_URL`, `USAGE_SERVICE_URL`
- `SPRING_AI_OLLAMA_BASE_URL`

You can override these in `docker-compose.yml` or by exporting environment variables before running Compose.

## Troubleshooting
- View logs: `docker compose logs -f <service>`
- If a service cannot reach MySQL, ensure the `mysql` container is healthy first.
- The first run may take time to download the Ollama image and the `deepseek-r1` model.

## Project Layout
- `alert-service/`, `device-service/`, `ingestion-service/`, `insight-service/`, `usage-service/`, `user-service/`
- `docker/` contains init scripts and Kafka data volume mapping.
- `docker-compose.yml` defines the full local stack.
