<h1 align="center">⚡️ Home IoT Energy Tracker</h1>

> Java Spring Boot microservice-based architecture to handle 500k+ users and 2.5m+ devices. Aggregates energy usage, analyzes + stores time-series IoT data, and produces AI-generated efficiency insights.

## System Design
<img width="900" alt="IoT Telemetry System Design" src="https://github.com/user-attachments/assets/c14cc644-de06-4550-b7cb-548b418fd094" />


## Grafana Observability

| JVM Metrics | Service Health Overview |
|---|---|
| <img alt="Grafana JVM Metrics" src="https://github.com/user-attachments/assets/fb54b941-55de-4d19-b0a1-071e29329a60" /> | <img alt="Grafana Service Health Overview" src="https://github.com/user-attachments/assets/bd373f3f-f867-47af-905a-bcafced74dfa" /> |

## Frontend Demo
---

## Tools Used
| Category | Tools |
| --- | --- |
| Backend |   <img src="https://img.shields.io/badge/Java-%23ED8B00?style=for-the-badge&logo=data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iaXNvLTg4NTktMSI/Pg0KPCEtLSBVcGxvYWRlZCB0bzogU1ZHIFJlcG8sIHd3dy5zdmdyZXBvLmNvbSwgR2VuZXJhdG9yOiBTVkcgUmVwbyBNaXhlciBUb29scyAtLT4NCjxzdmcgaGVpZ2h0PSI4MDBweCIgd2lkdGg9IjgwMHB4IiB2ZXJzaW9uPSIxLjEiIGlkPSJDYXBhXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIA0KCSB2aWV3Qm94PSIwIDAgNTAyLjYzMiA1MDIuNjMyIiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxnPg0KCTxnPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTI0MC44NjQsMjY5Ljg5NGMwLDAtMjguMDItNTMuOTkyLTI2Ljk4NS05My40NDVjMC43NTUtMjguMTkzLDY0LjMyNC01Ni4wNjIsODkuMjgxLTk2LjUyOQ0KCQkJQzMyOC4wNzQsMzkuNDMxLDMwMC4wNTQsMCwzMDAuMDU0LDBzNi4yMzQsMjkuMDc3LTEwLjM3Niw1OS4xNDdjLTE2LjYwOSwzMC4xMTMtNzcuOTE0LDQ3Ljc3OS0xMDEuNzQ5LDk5LjY3OQ0KCQkJUzI0MC44NjQsMjY5Ljg5NCwyNDAuODY0LDI2OS44OTR6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzQ1Ljc0MSwxMDUuODY5YzAsMC05NS40OTQsMzYuMzQ3LTk1LjQ5NCw3Ny44NDljMCw0MS41NDUsMjUuOTI4LDU1LjAyNywzMC4xMTMsNjguNTA5DQoJCQljNC4xNDIsMTMuNTI1LTcuMjY5LDM2LjM0Ny03LjI2OSwzNi4zNDdzMzcuMzYxLTI1Ljk1LDMxLjEwNS01Ni4wNjJjLTYuMjM0LTMwLjExMy0zNS4yOS0zOS40NzUtMTguNjU5LTY5LjU0NA0KCQkJQzI5Ni42NDYsMTQyLjc5OSwzNDUuNzQxLDEwNS44NjksMzQ1Ljc0MSwxMDUuODY5eiIvPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTIzMC41MSwzMjQuNzQ4Yzg4LjI0Ni0zLjE0OSwxMjAuNDMtMzAuOTk3LDEyMC40My0zMC45OTcNCgkJCWMtNTcuMDc2LDE1LjU1My0yMDguNjU0LDE0LjUzOS0yMDkuNzExLDMuMTI4Yy0xLjAxNC0xMS40MTEsNDYuNzAxLTIwLjc3Myw0Ni43MDEtMjAuNzczcy03NC43MjEsMC04MC45NTUsMTguNjgNCgkJCUMxMDAuNzQsMzEzLjQ2NywxNDIuMzI4LDMyNy44MzMsMjMwLjUxLDMyNC43NDh6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzU4LjE4NywzNjguNDk0YzAsMCw4Ni4zNjktMTguNDIxLDc3LjgyNy02NS4zMzhjLTEwLjM1NC01Ny4xMTktNzAuNTgtMjQuOTM2LTcwLjU4LTI0LjkzNg0KCQkJczQyLjYwMiwwLDQ2LjcyMiwyNS45MjhDNDE2LjMyLDMzMC4wOTgsMzU4LjE4NywzNjguNDk0LDM1OC4xODcsMzY4LjQ5NHoiLz4NCgkJPHBhdGggc3R5bGU9ImZpbGw6I2ZmZmZmZjsiIGQ9Ik0zMTUuNjI4LDM0My42MDFjMCwwLTIxLjc2NSw1LjcxNi01NC4wMTMsOS4zNGMtNDMuMjI4LDQuODUzLTk1LjQ5NCwxLjAxNC05OS42NTctNi4yNTYNCgkJCWMtNC4wOTgtNy4yNjksNy4yNjktMTEuNDExLDcuMjY5LTExLjQxMWMtNTEuOTIxLDEyLjQ2OC0yMy41MTIsMzQuMjMzLDM3LjMzOSwzOC40MThjNTIuMTU4LDMuNTU5LDEyOS43OTEtMTUuNTc0LDEyOS43OTEtMTUuNTc0DQoJCQlMMzE1LjYyOCwzNDMuNjAxeiIvPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTE4MS43MzgsMzg4Ljk0M2MwLDAtMjMuNTU1LDAuNjY5LTI0LjkzNiwxMy4xMzdjLTEuMzU5LDEyLjM4MiwxNC40OTYsMjMuNTEyLDcyLjY1LDI2Ljk2NA0KCQkJYzU4LjEzMywzLjQ1MSw5OC45ODgtMTUuODk4LDk4Ljk4OC0xNS44OThsLTI2LjI5NS0xNS45NjJjMCwwLTE2LjYzMSwzLjQ5NC00Mi4yMzYsNi45NDYNCgkJCWMtMjUuNjI2LDMuNDczLTc4LjE3My0yLjc4My04MC4yNDMtNy41OTNDMTc3LjU1MywzOTEuNjgyLDE4MS43MzgsMzg4Ljk0MywxODEuNzM4LDM4OC45NDN6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNNDA3Ljk5NCw0NDUuMDA1YzguOTk1LTkuNzA3LTIuNzgzLTE3LjMyMS0yLjc4My0xNy4zMjFzNC4xNDIsNC44NTMtMS4zMzcsMTAuMzc2DQoJCQljLTUuNTQ0LDUuNTIyLTU2LjA4NCwxOS4zNDktMTM3LjA2MSwyMy41MTJjLTgwLjk1NSw0LjE2My0xNjguODU2LTcuNjE1LTE3MS42MzktMTcuOTkNCgkJCWMtMi42OTYtMTAuMzc2LDQ1LjAxOC0xOC42NTksNDUuMDE4LTE4LjY1OWMtNS41MjIsMC42OS03MS45NiwyLjA3MS03NC4wNzQsMjAuMDgyYy0yLjA3MSwxNy45NjgsMjkuMDU2LDMyLjUwNywxNTMuNjcsMzIuNTA3DQoJCQlDMzQ0LjMzOSw0NzcuNDkxLDM5OS4wNDIsNDU0LjY0Nyw0MDcuOTk0LDQ0NS4wMDV6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzU5LjU2OCw0ODUuODE3Yy01NC42ODIsMTEuMDQ0LTIyMC43MzQsNC4wNzctMjIwLjczNCw0LjA3N3MxMDcuOTE5LDI1LjYyNiwyMzEuMTA5LDQuMTg1DQoJCQljNTguODg4LTEwLjI2OCw2Mi4zMTgtMzguNzYzLDYyLjMxOC0zOC43NjNTNDE0LjI1LDQ3NC43MDgsMzU5LjU2OCw0ODUuODE3eiIvPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCjwvZz4NCjwvc3ZnPg==&logoColor=white" alt="Java" />   <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-%236DB33F?style=for-the-badge&logo=springboot&logoColor=%23FFFFFF" /> <img alt="Static Badge" src="https://img.shields.io/badge/Spring%20AI-%23d4aa00?style=for-the-badge&logo=spring&logoColor=%23FFFFFF">|
| Frontend |   <img alt="Static Badge" src="https://img.shields.io/badge/typescript-%233178C6?style=for-the-badge&logo=typescript&logoColor=%23FFFFFF"> <img alt="Next.js" src="https://img.shields.io/badge/Next.js-black?style=for-the-badge&logo=nextdotjs&logoColor=white" />  <img alt="Static Badge" src="https://img.shields.io/badge/tailwind%20css-%2306B6D4?style=for-the-badge&logo=tailwindcss&logoColor=%23FFFFFF"> |
| Data Storage |   <img alt="Static Badge" src="https://img.shields.io/badge/MySQL%20(RDS)-%234479A1?style=for-the-badge&logo=mysql&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/InfluxDB-%2322ADF6?style=for-the-badge&logo=influxdb&logoColor=%23FFFFFF" /> <img alt="Static Badge" src="https://img.shields.io/badge/Flyway-%23CC0200?style=for-the-badge&logo=flyway&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/Redis-%23FF4438?style=for-the-badge&logo=redis&logoColor=%23FFFFFF"> |
| Messaging | <img alt="Static Badge" src="https://img.shields.io/badge/Apache%20Kafka%20(MSK)-%23231F20?style=for-the-badge&logo=apachekafka&logoColor=%23FFFFFF"> |
| AI Inference | <img src="https://img.shields.io/badge/AWS%20Bedrock-%2301a88d?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MDAiIGhlaWdodD0iODAwIiBmaWxsPSIjZmZmIiB2aWV3Qm94PSIwIDAgMzIgMzIiPjxwYXRoIGQ9Ik02LjU4NCA5LjAxYy0xLjM2IDAtMi43NC41My0yLjk3LjgyLS4wNi4xMi0uMiAxLjA5LjEzIDEuMDkuMTEgMCAuMTYuMDIuNDgtLjEzIDEuMi0uNDcgMS45Ni0uNDYgMi4wNy0uNDYgMS4zNS0uMTMgMi4xMy43OSAyLjAxIDEuOTh2LjdjLTEuMTQtLjI3LTEuNzktLjI4LTIuMTEtLjI4LTEuNjYtLjEtMy4xOTQuNzc2LTMuMTk0IDIuNyAwIDIuMTEgMS44ODMgMi41NiAyLjYxMyAyLjUzIDEuMDkuMDEgMi4xMy0uNDggMi44Mi0xLjMzLjU1IDEuMjMuOSAxLjE1LjkxIDEuMTUuMSAwIC4xOC0uMDQuMjYtLjA5bC41Ny0uNGMuMS0uMDYuMTgtLjE2LjE5LS4yOC0uMDEtLjI5LS41My0uNzQtLjQ5LTEuNzV2LTMuMTJhMy4xOCAzLjE4IDAgMCAwLS43OTktMi4zNSAzLjQyIDMuNDIgMCAwIDAtMi40OS0uNzhtMTkuMzczIDBjLTIgMC0zLjE1IDEuMjUtMy4xMiAyLjUyIDAgMS43NCAxLjc2IDIuMjkgMS45NiAyLjM1IDEuNjkuNTMgMS45Mi41NSAyLjM5Ljk1LjQuNDEuMzUgMS4yMS0uMjQgMS41Ni0uMTcuMS0uOS41NC0yLjU1LjItLjU1LS4xMS0uODQtLjI0LTEuMjktLjQzLS4xMi0uMDQtLjQtLjExLS40LjI2di40OWMwIC4yMy4xNC40NC4zNS41NCAxLjA1LjUzIDIuMzEuNTUgMi41OC41NS4wNCAwIDIuMzQuMDAxIDMuMTEtMS41NS4xNTgtLjMyLjU3LTEuNDktLjItMi40OS0uNjQtLjc1LTEuMTktLjgzLTIuODMtMS4zMy0uMTQtLjA0LTEuMzUtLjM1LTEuMzQtMS4yLS4wNi0xLjA5IDEuNDItMS4xNSAxLjczLTEuMTMgMS4yNS0uMDIgMS44Ny40NSAyLjIxLjQ4LjE1IDAgLjIyLS4wOS4yMi0uMjl2LS40NmEuNS41IDAgMCAwLS4wOS0uMzFjLS40LS41Mi0xLjkzLS43MS0yLjQ5LS43MW0tMTUuMTguMjVjLS4xMS4wMi0uMTkuMTMtLjE3LjI0LjAyLjEzLjA0LjI2LjA5LjM5bDIuMjQgNy4zOWMuMDUuMjQuMjEuNS41Ni40NmguODJjLjUuMDUuNTctLjQzLjU4LS40OGwxLjQ3LTYuMTYgMS40OSA2LjE3Yy4wMS4wNS4wOC41My41Ny40OGguODNjLjM2LjA0LjUzLS4yMi41OC0uNDYgMi41Mi04LjExIDIuMzUtNy41NiAyLjM3LTcuNjQuMDQtLjQyLS4yLS4zOS0uMjQtLjM4aC0uODljLS40NS0uMDUtLjU0LjM2LS41Ni40NmwtMS42NiA2LjQxLTEuNS02LjQxYy0uMDctLjQ5LS40Ny0uNDctLjU3LS40NmgtLjc3Yy0uNDQtLjA0LS41NS4zMS0uNTguNDZsLTEuNDkgNi4zMi0xLjYtNi4zMmMtLjA0LS4yLS4xNy0uNTEtLjU2LS40N3ptLTQuMjU0IDQuNjNjLjcyLjAxIDEuMzQyLjEyIDEuNzcyLjIyIDAgLjUuMDE4Ljc4LS4wOTIgMS4yMy0uMTQuNDgtLjc1OSAxLjM1LTIuMjE5IDEuMzctLjg0LjA0LTEuMzktLjYyLTEuMzQtMS4zNy0uMDUtMS4yIDEuMTktMS41IDEuODgtMS40NW0yMi41MTggNi4xMTJjLS45MzMuMDEzLTIuMDM1LjIyMi0yLjg3MS44MDktLjI1OC4xNzktLjIxMy40MjcuMDc0LjM5NC45NC0uMTEzIDMuMDMyLS4zNjcgMy40MDYuMTExcy0uNDE0IDIuNDUtLjc2MyAzLjMzMmMtLjEwOC4yNjMuMTIuMzcyLjM2MS4xNzIgMS41NjQtMS4zMSAxLjk3LTQuMDU2IDEuNjUtNC40NS0uMTYtLjE5OC0uOTI0LS4zODEtMS44NTctLjM2OG0tMjcuODI0IDFjLS4yMTguMDMtLjMxMi4zMDYtLjA4NC41MjVDNS4wNSAyNS4yMDEgMTAuMjI2IDI3IDE1Ljk3MyAyN2M0LjA5OSAwIDguODU3LTEuMzM3IDEyLjE0Mi0zLjg1Ny41NDMtLjQyLjA4LTEuMDQ3LS40NzYtLjgtMy42ODMgMS42MjYtNy42ODQgMi40MDktMTEuMzI1IDIuNDA5LTUuMzk2IDAtMTAuNjItMS4xMjctMTQuODQ1LTMuNjg2YS40LjQgMCAwIDAtLjI1Mi0uMDY0Ii8+PC9zdmc+&logoColor=white" alt="AWS" /> <img alt="Static Badge" src="https://img.shields.io/badge/Ollama-%23000000?style=for-the-badge&logo=ollama&logoColor=%23FFFFFF"> |
| Observability | <img alt="Static Badge" src="https://img.shields.io/badge/prometheus-%23E6522C?style=for-the-badge&logo=prometheus&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/grafana-%23F46800?style=for-the-badge&logo=grafana&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/loki-%23D96800?style=for-the-badge&logoColor=%23FFFFFF"> |
| Infrastructure |   <img alt="Static Badge" src="https://img.shields.io/badge/kubernetes%20(eks)-%23326CE5?style=for-the-badge&logo=kubernetes&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/helm-%230F1689?style=for-the-badge&logo=helm&logoColor=%23FFFFFF"> <img alt="Static Badge" src="https://img.shields.io/badge/Docker-%232496ED?style=for-the-badge&logo=docker&logoColor=%23FFFFFF" />     <img alt="Static Badge" src="https://img.shields.io/badge/terraform-%23844FBA?style=for-the-badge&logo=terraform&logoColor=%23FFFFFF">   <img alt="Static Badge" src="https://img.shields.io/badge/GitHub%20Actions-%232088FF?style=for-the-badge&logo=githubactions&logoColor=%23FFFFFF">|

## Technical Highlights
- Event-driven pipeline with **Kafka decoupling** ingestion, processing, and alerting across three topics.
- **Dual persistence model**: MySQL for relational data (users, devices, alerts) and InfluxDB for time-series usage analytics.
- **MySQL primary + read replica** with async GTID row-based replication (`super_read_only=ON` on the replica). `user-service`, `device-service`, and `alert-service` route reads to the replica via Spring's `AbstractRoutingDataSource` keyed off `@Transactional(readOnly=true)`, with separate Hikari pools (`primary` size 8, `replica` size 4) and a `LazyConnectionDataSourceProxy` so the routing key resolves after the transaction flag is set. Flyway is pinned to the primary pool.
- **Redis caching layer**: usage-service caches InfluxDB query results in Redis (2-minute TTL) and uses a distributed `SETNX` lock on the 10-second aggregation scheduler, ensuring only one replica hits InfluxDB per tick regardless of how many instances are running.
- **AI-powered insights** via Spring AI + Ollama (gemma3:4b), polling usage aggregates on a cron schedule to generate efficiency recommendations.
- **Multi-threaded** simulation in ingestion-service to stress test throughput and backpressure locally.
- **Full observability stack**: distributed tracing (OTLP → Tempo), structured log aggregation (ECS JSON → Promtail → Loki), and Prometheus metrics — all correlated in Grafana.
- **Spring Boot version split**: 5 services on Boot 4.0.1, insight-service on Boot 3.5.9 (Spring AI 1.1.2 does not yet support Boot 4.x).
- **Production-grade Kubernetes deployment** on AWS EKS with Terraform IaC, HPA autoscaling (2–5 replicas), PodDisruptionBudgets, NetworkPolicies (default-deny), and CloudWatch alarms.
- **Fully automated CI/CD**: GitHub Actions OIDC → ECR push → Helm deploy, with change detection (only rebuilds modified services).
- **Zero-secret codebase**: all credentials in AWS Secrets Manager, synced to K8s via External Secrets Operator (IRSA-bound).

## Data Flow

```
ingestion-service  →  [Kafka: energy-usage]  →  usage-service  →  InfluxDB
                                                      ↓                ↑
                                                    Redis  ─(miss)─────┘
                                                      ↓
                                             [Kafka: energy-alerts]
                                                      ↓
                                               alert-service  →  MySQL + Mailpit

insight-service  →  (polls usage-service REST)  →  LLM  →  AI insights
                                                          ├── Ollama gemma3:4b (local/Minikube)
                                                          └── AWS Bedrock Claude Haiku 4.5 (EKS)
```

## Services

| Service | Port | Persistence | Notes |
|---|---|---|---|
| user-service | 8080 | MySQL (primary + replica) | Flyway migrations, LoggingAspect, ExecutionTimeAspect, replica-routed reads |
| device-service | 8081 | MySQL (primary + replica) | Inter-service calls to user-service, replica-routed reads |
| ingestion-service | 8082 | — | Kafka producer, multi-threaded event simulator |
| usage-service | 8083 | InfluxDB, Redis | Kafka consumer, Redis read cache + scheduler lock, emits alert events |
| alert-service | 8084 | MySQL (primary + replica) | Kafka consumer, Spring Mail via Mailpit, replica-routed reads |
| insight-service | 8085 | — | Spring AI + Ollama (local) or Bedrock (EKS), polls usage-service |

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
| InfluxDB UI | `http://localhost:8072` (iot-energy / password) |
| Mailpit (email UI) | `http://localhost:8025` |
| MySQL (primary) | `localhost:3307` (root / password) |
| MySQL (replica, read-only) | `localhost:3308` (root / password) |
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

Deploys: MySQL (primary + read replica StatefulSets, async GTID replication), Kafka, InfluxDB, Mailpit, Kafka UI, and Ollama (GPU-backed StatefulSet with PVC).

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

## CI/CD

### Pipeline Overview

```
push to main / tag v*
    │
    ▼
build-and-push.yml
    ├── detect-changes      ← dorny/paths-filter scans services/** + frontend/
    ├── build matrix        ← one runner per changed service (parallel)
    │     ├── OIDC → assume iot-tracker-dev-gha-deploy role
    │     ├── docker buildx → push to ECR Public
    │     └── tags: <sha> + latest (insight-service: +bedrock variant)
    ▼
deploy-eks.yml (auto-triggered or manual)
    ├── OIDC → assume deploy role
    ├── helm upgrade infra (values-eks.yaml)
    ├── helm upgrade observability (values-eks.yaml)
    └── helm upgrade microservices (values-eks.yaml, --set image.tag=<sha>)
```

- **No stored credentials** — all AWS access via GitHub OIDC federated identity.
- **Change detection** — only rebuilds services with actual file changes.
- **SHA-pinned deploys** — EKS never runs `:latest`, always a specific commit SHA.
- **Concurrency guard** — only one deploy-eks run at a time.

### Manual Operations

```bash
# Rebuild a single service from any branch:
gh workflow run build-test.yml --ref <branch> -f service=<service-name>

# Deploy to EKS manually:
gh workflow run deploy-eks.yml
```

### Automated Code Review

Every pull request triggers a Claude Code review (`.github/workflows/claude-review.yml`). Claude checks against `CLAUDE.md` conventions and posts inline feedback. Mention `@claude` in a PR comment for targeted review.

## Real Usage

I connected the system to my own home using Shelly smartplugs. Check out the `/shelly` folder for how you can do it too!

## Run on AWS EKS (Production)

The application runs on AWS EKS with managed infrastructure (RDS, MSK Serverless) and full production hardening. All infrastructure is defined in Terraform; deployments use the same Helm charts with EKS-specific value overlays.

### Architecture

```
                          Route53 (energy.aidanchien.com)
                                     │
                                ACM TLS cert
                                     │
                              NLB (internet-facing)
                                     │
                            ingress-nginx controller
                                     │
            ┌────────────────────────────────────────────────┐
            │           │            │           │            │
        frontend   user-svc    device-svc   usage-svc    ... (7 svcs)
            │           │            │           │            │
            └── inter-service via ClusterIP DNS ─────────────┘
                         │                    │
                         ▼                    ▼
                     RDS MySQL           MSK Serverless
                    (managed)           (IAM auth, 9098)

            In-cluster (EBS gp3 persistence):
            - InfluxDB, Redis, Mailpit
            - Prometheus, Grafana, Loki, Tempo

            insight-service ──► AWS Bedrock (Claude Haiku 4.5)
                                via cross-region inference profile (IRSA)
```

### AWS Resources (Terraform)

| Resource | Type | Details |
|----------|------|---------|
| VPC | `10.20.0.0/16` | 3 public, 3 private, 3 DB subnets, NAT gateway |
| EKS | `iot-tracker-dev` | K8s 1.31, managed node group (t3.large × 3) |
| RDS MySQL | `db.t3.medium` | 8.0.44, encrypted, 7-day backup, Performance Insights |
| MSK Serverless | IAM auth | SASL/SSL on port 9098, topics: `energy-usage`, `energy-alerts` |
| Route53 | Public zone | `energy.aidanchien.com` + wildcard |
| ACM | TLS cert | `energy.aidanchien.com` + `*.energy.aidanchien.com` (Let's Encrypt via cert-manager) |
| Secrets Manager | 3 secrets | `iot/dev/rds/master`, `iot/dev/global`, `iot/dev/frontend` |
| IRSA roles | 7 roles | ALB Controller, EBS CSI, ESO, ExternalDNS, cert-manager, insight-service, GHA deploy |
| CloudWatch | 4 alarms | RDS CPU >80%, RDS connections >80% max, RDS storage <20%, EKS node health |
| SNS | 1 topic | `iot-tracker-dev-alarms` — alarm notification target |
| ECR Public | 8 repos | `public.ecr.aws/v6r1m8q2/energy-tracker/<service>` |

### Cluster Addons

Installed via `scripts/eks-bootstrap.sh`:

- **EBS CSI driver** — gp3 StorageClass (default)
- **AWS Load Balancer Controller** — provisions NLB for ingress-nginx
- **ingress-nginx** — internet-facing NLB, handles TLS termination
- **ExternalDNS** — auto-creates Route53 A records from Ingress resources
- **External Secrets Operator** — syncs AWS Secrets Manager → K8s Secrets
- **cert-manager** — DNS-01 challenges via Route53 for Let's Encrypt certs
- **metrics-server** — enables `kubectl top` and HPA CPU metrics

### Production Hardening

| Feature | Configuration |
|---------|--------------|
| **HPA** | All 7 services autoscale: min 2, max 5 replicas, target 70% CPU |
| **PDB** | All 7 services: minAvailable=1 (survives node drain) |
| **NetworkPolicies** | Default-deny + per-service ingress/egress allowlists |
| **CloudWatch Alarms** | RDS CPU >80%, connections >53/66 max, free storage <4 GiB, EKS failed nodes |
| **TLS everywhere** | Let's Encrypt cert, TLS termination at ingress |
| **Secrets in Secrets Manager** | No plaintext in values files; synced via ExternalSecrets |
| **IRSA** | Pod-level IAM — insight-service has Bedrock access only |

### NetworkPolicy Rules

| Service | Inbound from | Outbound to |
|---------|-------------|-------------|
| user-service | ingress, device-svc, usage-svc, insight-svc, frontend | RDS (3306) |
| device-service | ingress, usage-svc | RDS (3306), user-svc (8080) |
| ingestion-service | ingress | MSK (9098) |
| usage-service | ingress, insight-svc | MSK (9098), InfluxDB (8086), user-svc, device-svc |
| alert-service | ingress | MSK (9098), RDS (3306), Mailpit (1025) |
| insight-service | ingress | usage-svc (8083), Bedrock (443) |
| frontend | ingress | user-svc (8080) |

### CI/CD (GitHub Actions)

| Workflow | Trigger | What it does |
|----------|---------|-------------|
| `build-and-push.yml` | Push to `main` or `v*` tag | Detects changed services via `dorny/paths-filter`, builds Docker images, pushes to ECR with SHA + latest tags. insight-service dual-builds: ollama + bedrock variants. |
| `deploy-eks.yml` | After successful build, or manual `workflow_dispatch` | OIDC auth → `helm upgrade` infra → observability → microservices with SHA-pinned image tags |
| `build-test.yml` | Manual `workflow_dispatch` | Single-service rebuild utility (uses `environment:dev` for OIDC from any branch) |

All workflows use **GitHub OIDC** → IAM role assumption (no stored AWS credentials).

### Prerequisites

- Terraform ≥ 1.5
- AWS CLI v2 with credentials for account `714454206433`
- `kubectl`, `helm` 3.x
- Domain with NS delegation to Route53

### Deploy from Scratch

```bash
# 1. Provision infrastructure (~8 min)
cd terraform/envs/dev
terraform init && terraform apply

# 2. Configure kubectl
aws eks update-kubeconfig --name iot-tracker-dev --region us-east-1

# 3. Install cluster addons
./scripts/eks-bootstrap.sh

# 4. Populate secrets
aws secretsmanager put-secret-value --secret-id iot/dev/global \
  --secret-string '{"SPRING_DATASOURCE_PASSWORD":"<rds-password>","INFLUX_TOKEN":"my-token","JWT_SECRET":"<jwt-secret>"}'
aws secretsmanager put-secret-value --secret-id iot/dev/frontend \
  --secret-string '{"NEXTAUTH_SECRET":"<secret>","GOOGLE_CLIENT_ID":"<id>","GOOGLE_CLIENT_SECRET":"<secret>"}'

# 5. Deploy Helm charts
helm upgrade --install infra ./k8s/charts/infra-chart -f ./k8s/charts/infra-chart/values-eks.yaml
helm upgrade --install observability ./k8s/charts/observability-chart -f ./k8s/charts/observability-chart/values-eks.yaml
helm upgrade --install microservices ./k8s/charts/microservices-chart -f ./k8s/charts/microservices-chart/values-eks.yaml

# 6. Verify
kubectl get pods   # All pods Running
kubectl get hpa    # CPU targets reporting
kubectl get pdb    # ALLOWED DISRUPTIONS ≥ 1
```

### Teardown

```bash
helm uninstall microservices
helm uninstall observability
helm uninstall infra
cd terraform/envs/dev && terraform destroy
```

### Cost Estimate (dev)

| Component | Monthly (approx) |
|-----------|-----------------|
| EKS control plane | $73 |
| 3× t3.large nodes | $180 |
| RDS db.t3.medium | $50 |
| MSK Serverless | $10–30 (usage-based) |
| NAT Gateway | $35 |
| EBS volumes (gp3) | $25 |
| NLB | $20 |
| **Total** | **~$400/mo** |

---

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
  └── charts/microservices-chart/  # All 7 services + shared ConfigMap/Secret/Ingress
terraform/
  └── envs/dev/         # VPC, EKS, RDS, MSK, IAM, Route53, ACM, CloudWatch Alarms
scripts/
  └── eks-bootstrap.sh  # Installs all EKS cluster addons (Phase 2)
.github/workflows/
  ├── build-and-push.yml    # CI: build + push to ECR on merge
  ├── deploy-eks.yml        # CD: deploy to EKS via Helm
  ├── build-test.yml        # Manual single-service rebuild
  └── claude-review.yml     # AI code review on PRs
docker-compose.yml                 # Core application stack
docker-compose.observability.yml   # Prometheus, Grafana, Loki, Promtail, Tempo
eks_plan.md                        # EKS migration plan + phase progress
```
