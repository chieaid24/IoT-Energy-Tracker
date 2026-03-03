<h1 align="center">⚡️ Home IoT Energy Tracker</h1>

> Java Spring Boot microservice-based architecture to handle 500k+ users and 2.5m+ devices. Aggregates energy usage, analyzes + stores time-series IoT data, and produces AI-approved efficiency insights. 

## System Design
<img width="6288" height="4516" alt="IoT Telemetry System Design4" src="https://github.com/user-attachments/assets/74640047-1b3c-47b9-98df-3fb663cf7181" />

## Tools / Frameworks Used
| Category | Tools |
| --- | --- |
| Language / Frameworks | Java 21 (Maven), Spring Boot, Spring Actuator |
| Database | MySQL, InfluxDB, Flyway |
| Microservices | Kafka, Ollama (qwen2.5:0.5b) |
| Dev Tools | Docker, Docker Compose, Mailpit |

## Technical Highlights
- Event-driven pipeline with **Kafka decoupling** ingestion, processing, and alerting.
- Dual persistence model: **MySQL** for relational data and **InfluxDB** for time-series usage analytics.
- AI-powered insights service that integrates **Ollama** (qwen2.5:0.5b) for actionable recommendations.
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

> Requires an NVIDIA GPU with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) installed for Ollama GPU acceleration.

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

## Run With Kubernetes (Minikube)

> Requires: [Minikube](https://minikube.sigs.k8s.io/docs/start/), [Helm](https://helm.sh/docs/intro/install/), [kubectl](https://kubernetes.io/docs/tasks/tools/), and an NVIDIA GPU with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) installed.

### 1. Start Minikube

```bash
minikube start --gpus all --driver=docker --memory=8192 --cpus=6
```

### 2. Enable the Ingress Addon

```bash
minikube addons enable ingress
```

### 3. Install the Infrastructure Chart

```bash
helm install infra ./k8s/charts/infra-chart
```

This deploys: MySQL, Kafka, InfluxDB, Mailpit, Kafka UI, and Ollama (with GPU acceleration).

Watch until all pods are running:
```bash
kubectl get pods -w
```

Ollama will take several minutes on first start since it pulls and warms up the `qwen2.5:0.5b` model. Watch progress:
```bash
kubectl logs statefulset/infra-ollama -f
```

### 4. Install the Microservices Chart

```bash
helm install microservices ./k8s/charts/microservices-chart
```

This deploys: user-service, device-service, ingestion-service, usage-service, alert-service, and insight-service.

Watch until all pods are running (or use `minikube dashboard`):
```bash
kubectl get pods -w
```

### 5. Start Minikube Tunnel

In a separate terminal, run and keep open:
```bash
minikube tunnel
```

This exposes LoadBalancer services and the ingress to `localhost`.

### Service URLs

| Service | URL |
|---|---|
| Microservices API (via ingress) | `http://localhost/api/v1/...` |
| Kafka UI | `http://localhost:8080` |
| Mailpit (email UI) | `http://localhost:8025` |
| InfluxDB UI | `http://localhost:8086` |
| MySQL | `localhost:3307` (root / password) |

### Upgrade & Restart

After changing chart values:
```bash
helm upgrade infra ./k8s/charts/infra-chart
helm upgrade microservices ./k8s/charts/microservices-chart
```

### Teardown

```bash
helm uninstall microservices
helm uninstall infra
minikube stop
```

---

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

## Extensions (in progress)
- Add Next.js frontend to start/stop simulation, and run within the cluster.
- Redis caching layer, CDN for static content, and queue for AI inference requests.
- Stronger AI guardrails to reduce hallucinations and improve validity
- MySQL read replicas
- Migrate cluster to AWS EKS with IAM, ALB/NLB ingress, and use more cloud-native, decoupled services (ex. convert `alerting-service` to a Lambda function, MSK instead of Kafka, etc)
