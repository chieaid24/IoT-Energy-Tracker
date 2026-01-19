# Getting Started

## Run with Docker
Build the service images:
```
docker compose build
```

Start the stack:
```
docker compose up -d
```

Stop the stack:
```
docker compose down
```

## Health checks
Each service exposes Spring Actuator health at:
- http://localhost:8080/actuator/health (user-service)
- http://localhost:8081/actuator/health (device-service)
- http://localhost:8082/actuator/health (ingestion-service)
- http://localhost:8083/actuator/health (usage-service)
- http://localhost:8084/actuator/health (alert-service)
- http://localhost:8085/actuator/health (insight-service)

## Notes
- The first `docker compose up -d` will pull the Ollama image and download the `deepseek-r1` model, which can take a while.
