<h1 align="center">⚡️ Home IoT Energy Tracker</h1>

> Java Spring Boot microservice-based architecture to handle 500k+ users and 2.5m+ devices. Aggregates energy usage, analyzes + stores time-series IoT data, and produces AI-generated efficiency insights.

## System Design
<img width="6288" height="4516" alt="IoT Telemetry System Design" src="https://github.com/user-attachments/assets/74640047-1b3c-47b9-98df-3fb663cf7181" />

## Grafana Observability

| JVM Metrics | Service Health Overview |
|---|---|
| <img alt="Grafana JVM Metrics" src="https://github.com/user-attachments/assets/fb54b941-55de-4d19-b0a1-071e29329a60" /> | <img alt="Grafana Service Health Overview" src="https://github.com/user-attachments/assets/bd373f3f-f867-47af-905a-bcafced74dfa" /> |

## Tools / Frameworks Used
| Category | Tools |
| --- | --- |
| Language / Frameworks | Java 21 (Maven), Spring Boot, Spring Actuator, Spring AI |
| Database | MySQL, InfluxDB, Flyway |
| Caching | Redis |
| Messaging | Apache Kafka |
| AI Inference | Ollama (gemma3:4b) |
| Observability | Prometheus, Grafana, Loki, Promtail, Tempo (OpenTelemetry) |
| Frontend | Next.js 16, TypeScript, Tailwind CSS v4, shadcn/ui |
| Dev Tools | Docker, Docker Compose, Mailpit |
| Orchestration | Kubernetes (Minikube), Helm |

## Technical Highlights
- Event-driven pipeline with **Kafka decoupling** ingestion, processing, and alerting across three topics.
- **Dual persistence model**: MySQL for relational data (users, devices, alerts) and InfluxDB for time-series usage analytics.
- **Redis caching layer**: usage-service caches InfluxDB query results in Redis (2-minute TTL) and uses a distributed `SETNX` lock on the 10-second aggregation scheduler, ensuring only one replica hits InfluxDB per tick regardless of how many instances are running.
- **AI-powered insights** via Spring AI + Ollama (gemma3:4b), polling usage aggregates on a cron schedule to generate efficiency recommendations.
- **Multi-threaded** simulation in ingestion-service to stress test throughput and backpressure locally.
- **Full observability stack**: distributed tracing (OTLP → Tempo), structured log aggregation (ECS JSON → Promtail → Loki), and Prometheus metrics — all correlated in Grafana.
- **Spring Boot version split**: 5 services on Boot 4.0.1, insight-service on Boot 3.5.9 (Spring AI 1.1.2 does not yet support Boot 4.x).
- **Production-grade Kubernetes deployment** via Helm with init containers, health probes, shared ConfigMaps/Secrets, and ingress routing.

## Data Flow

```
ingestion-service  →  [Kafka: energy-usage]  →  usage-service  →  InfluxDB
                                                      ↓                ↑
                                                    Redis  ─(miss)─────┘
                                                      ↓
                                             [Kafka: energy-alerts]
                                                      ↓
                                               alert-service  →  MySQL + Mailpit

insight-service  →  (polls usage-service REST)  →  Ollama (gemma3:4b)  →  AI insights
```

## Services

| Service | Port | Persistence | Notes |
|---|---|---|---|
| user-service | 8080 | MySQL | Flyway migrations, LoggingAspect, ExecutionTimeAspect |
| device-service | 8081 | MySQL | Inter-service calls to user-service |
| ingestion-service | 8082 | — | Kafka producer, multi-threaded event simulator |
| usage-service | 8083 | InfluxDB, Redis | Kafka consumer, Redis read cache + scheduler lock, emits alert events |
| alert-service | 8084 | MySQL | Kafka consumer, Spring Mail via Mailpit |
| insight-service | 8085 | — | Spring AI + Ollama, polls usage-service |

## Observability

All 6 services are fully instrumented across three observability pillars, correlated in Grafana.

### Architecture

```
Spring Boot Services
  ├── /actuator/prometheus  ──────────────► Prometheus ─────► Grafana
  ├── OTLP traces (HTTP :4318) ──────────► Tempo ──────────► Grafana
  └── stdout JSON (ECS format) ─► Promtail ─► Loki ─────────► Grafana
```

### Metrics
- Micrometer + `micrometer-registry-prometheus` on all services.
- Prometheus scrapes `/actuator/prometheus` from all 6 services every 15s.
- JVM, HTTP, and Kafka consumer/producer metrics collected out of the box.

### Distributed Tracing
- Boot 4.x services: `spring-boot-starter-opentelemetry` — traces pushed via OTLP HTTP to Tempo.
- Boot 3.x (insight-service): `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`.
- Kafka observation enabled: trace context propagated across all producer/consumer spans.
- 100% sampling rate in local dev (`MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0`).

### Logs
- All services emit **ECS-formatted JSON** to stdout (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`).
- Promtail uses Docker SD to collect container logs and extract `level`, `service`, `traceId`, and `spanId` as Loki labels.
- Log-to-trace correlation works in Grafana — click a log line to jump to the matching Tempo trace.

### Dashboards (provisioned, no manual setup)
| Dashboard | Description |
|---|---|
| Service Health Overview | HTTP request rates, error rates, latency per service |
| JVM Metrics | Heap, GC pause, thread count per service |
| Kafka Event Pipeline | Producer/consumer lag, throughput for `energy-usage` and `energy-alerts` topics |
| IoT Business Metrics | Device count, energy readings, alert frequency |

### Observability Ports (Docker Compose)
| Service | Port |
|---|---|
| Prometheus | `localhost:9090` |
| Grafana | `localhost:3001` (iot-energy / password) |
| Loki | `localhost:3100` |
| Tempo | `localhost:3200` |

## Frontend

Next.js 16 dashboard (App Router, TypeScript, Tailwind CSS v4, shadcn/ui) served through nginx reverse proxy.

### Authentication & Security

- **Google OAuth 2.0** via NextAuth.js — Google ID tokens are forwarded to `user-service /api/v1/auth/google`, which validates them against Google's `tokeninfo` endpoint and verifies audience (`aud`) claim against the configured client ID. New users are auto-provisioned on first Google sign-in.
- **Credentials login** — email/password verified against BCrypt-hashed passwords in MySQL. Passwords are never stored in plaintext.
- **JWT session strategy** — on successful login (either provider), user-service issues an HMAC-SHA signed JWT (24h expiry) containing `userId`, `email`, and `name`. NextAuth stores this token server-side in an encrypted session cookie and attaches it as a `Bearer` token on all backend API calls via a server-side `fetchApi` helper.
- **Route protection** — NextAuth middleware redirects unauthenticated users to `/login` for all `/dashboard/*` routes. Client-side `useSession` provides an additional guard.
- **Stateless backend** — Spring Security configured with `SessionCreationPolicy.STATELESS` and CSRF disabled (API-only, no browser session cookies).

### Pages

| Route | Description |
|---|---|
| `/login` | Email/password + Google OAuth sign-in |
| `/register` | Account registration (auto-signs in on success) |
| `/dashboard` | Summary cards, energy bar chart (Recharts), AI insights panel |
| `/dashboard/devices` | Device table, embedded InfluxDB explorer |
| `/dashboard/alerts` | Embedded Mailpit inbox for email alerts |

### Dashboard Components

- **Summary Cards** — device count, 7-day energy consumption (kWh), and alert count with animated bell on new alerts. Energy/device data polls every 5s, alerts poll every 1s.
- **Energy Chart** — per-device 7-day energy consumption bar chart via Recharts, polling usage-service every 5s.
- **AI Insights Panel** — Ollama-generated energy efficiency tips with confidence score, cached in localStorage, with manual regenerate button.
- **Device Table** — lists all user devices with type badges.
- **InfluxDB Explorer** — embedded InfluxDB UI iframe for direct time-series data exploration.
- **Mailpit Inbox** — embedded Mailpit iframe showing alert emails sent by alert-service.

## Run With Docker

> Requires an NVIDIA GPU with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) installed for Ollama GPU acceleration.

Start the core stack:
```bash
docker compose up -d
```

Start the observability stack (separate compose file, shares `iot-network`):
```bash
docker compose -f docker-compose.observability.yml up -d
```

Check status:
```bash
docker compose ps
```

Stop both stacks:
```bash
docker compose down
docker compose -f docker-compose.observability.yml down
```

### Service URLs

Ports are consistent across Docker Compose and Kubernetes (via `minikube tunnel`).

| Service | URL |
|---|---|
| user-service | `http://localhost:8080` |
| device-service | `http://localhost:8081` |
| ingestion-service | `http://localhost:8082` |
| usage-service | `http://localhost/api/v1/usage` (via nginx) |
| alert-service | `http://localhost:8084` |
| insight-service | `http://localhost:8085` |
| Microservices API (K8s ingress) | `http://localhost/api/v1/...` |
| Kafka UI | `http://localhost:8070` |
| InfluxDB UI | `http://localhost:8072` |
| Mailpit (email UI) | `http://localhost:8025` |
| MySQL | `localhost:3307` (root / password) |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3001` (iot-energy / password) |
| Loki | `http://localhost:3100` |
| Tempo | `http://localhost:3200` |

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

Deploys: MySQL, Kafka, InfluxDB, Mailpit, Kafka UI, and Ollama (GPU-backed StatefulSet with PVC).

Watch until all pods are running:
```bash
kubectl get pods -w
```

Ollama pulls and warms up `gemma3:4b` on first start — watch progress:
```bash
kubectl logs statefulset/infra-ollama -f
```

### 4. Install the Observability Chart

```bash
helm install observability ./k8s/charts/observability-chart
```

Deploys: Prometheus, Grafana, Loki, Promtail (DaemonSet), and Tempo.

Watch until all pods are running:
```bash
kubectl get pods -w
```

### 5. Install the Microservices Chart

```bash
helm install microservices ./k8s/charts/microservices-chart
```

Deploys all 6 microservices with init containers that gate startup on their dependencies (MySQL, Kafka, upstream services).

### 6. Start Minikube Tunnel

In a separate terminal, run and keep open:
```bash
minikube tunnel
```

This exposes LoadBalancer services and the ingress to `localhost`.

### Upgrade & Teardown

```bash
# After changing chart values:
helm upgrade infra ./k8s/charts/infra-chart
helm upgrade observability ./k8s/charts/observability-chart
helm upgrade microservices ./k8s/charts/microservices-chart

# Teardown:
helm uninstall microservices
helm uninstall observability
helm uninstall infra
minikube stop
```

---

## Health Checks

Each service exposes Spring Actuator endpoints:

```
http://localhost:{port}/actuator/health
http://localhost:{port}/actuator/prometheus
```

Ports: user-service `8080`, device-service `8081`, ingestion-service `8082`, usage-service `8083`, alert-service `8084`, insight-service `8085`.

## Project Layout

```
services/               # Six Spring Boot microservices
frontend/               # Next.js 16 frontend (App Router, Tailwind v4, shadcn/ui)
observability/
  ├── prometheus/       # prometheus.yml — scrape configs for all services
  ├── grafana/          # Provisioned datasources (Prometheus, Loki, Tempo) + 4 dashboards
  ├── loki/             # loki.yaml — in-memory ring, TSDB storage
  ├── promtail/         # promtail.yaml — Docker SD, ECS JSON pipeline
  └── tempo/            # tempo.yaml — OTLP receivers, local storage
k8s/
  ├── charts/infra-chart/          # MySQL, Kafka, InfluxDB, Mailpit, Kafka UI, Ollama
  ├── charts/observability-chart/  # Prometheus, Grafana, Loki, Promtail, Tempo
  └── charts/microservices-chart/  # All 6 microservices + shared ConfigMap/Secret/Ingress
docker-compose.yml                 # Core application stack
docker-compose.observability.yml   # Prometheus, Grafana, Loki, Promtail, Tempo
```

## CI/CD

### Pipeline Overview

On every push to `main`, a GitHub Actions workflow (`.github/workflows/ci.yaml`) detects which services changed and builds only those — avoiding full rebuilds on every commit.

```
push to main
    │
    ▼
detect-changes          ← dorny/paths-filter scans services/**
    │                      normalizes paths → unique service directories
    ▼
build-and-push          ← matrix job, one runner per changed service (parallel)
    │  configure AWS credentials
    │  login to Amazon ECR Public
    │  docker build <service>/
    │  docker push public.ecr.aws/<alias>/energy-tracker/<service>:latest
    ▼
ECR Public registry     ← image available at :latest tag
```

### Deploying Updated Images to Kubernetes

After new images are pushed, apply them to the cluster with:

```bash
helm upgrade microservices ./k8s/charts/microservices-chart
```

Kubernetes will pull the updated `:latest` images from ECR and perform a rolling restart of the affected deployments.

### Automated Code Review

Every pull request opened against `main` triggers a Claude Code review (`.github/workflows/claude-review.yml`). Claude checks against the project's `CLAUDE.md` conventions and posts inline feedback on the PR. Reviewers can also mention `@claude` in any PR comment to request a targeted review.

## Real Usage

I connected the system to my own home using Shelly smartplugs. Check out the `/shelly` folder for how you can do it too!

---

## Extensions (in progress)
- **Redis queue** for AI inference requests (rate-limiting and request queuing for Ollama).
- **MySQL read replicas** for query scalability.
- **AWS EKS migration**: IAM, ALB/NLB ingress, MSK (managed Kafka), alert-service as Lambda.
