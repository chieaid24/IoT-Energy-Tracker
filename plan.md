# Kubernetes Migration Plan: Remaining Services

## Context
The project is partially migrated to Helm/Minikube. MySQL, user-service, and device-service are already deployed. The goal is to migrate the remaining 4 infrastructure services (Kafka, InfluxDB, Mailpit, Ollama) and 4 microservices (ingestion, usage, alert, insight) following the exact same patterns already established in the codebase.

**Naming contracts that must be preserved:**
- `helm install infra ./infra-chart` → infra services become `infra-<name>.default.svc.cluster.local`
- `helm install microservices ./microservices-chart` → microservices become `microservices-<name>.default.svc.cluster.local`

---

## Current State

| Component | Status |
|---|---|
| MySQL | ✅ infra-chart subchart |
| user-service | ✅ microservices-chart subchart |
| device-service | ✅ microservices-chart subchart |
| Kafka | ✅ infra-chart subchart |
| InfluxDB | ✅ infra-chart subchart |
| Mailpit | ✅ infra-chart subchart |
| Kafka UI | ✅ infra-chart subchart |
| ingestion-service | ❌ Phase 2 |
| alert-service | ❌ Phase 3 |
| usage-service | ❌ Phase 4 |
| Ollama | ❌ Phase 5 |
| insight-service | ❌ Phase 6 |

---

## Dependency Graph

```
MySQL (✅)              Kafka (✅)              InfluxDB (✅)    Mailpit (✅)    Ollama (P5)
  ├── user-svc (✅)       ├── ingestion (P2)      └── usage (P4)    └── alert (P3)    └── insight (P6)
  ├── device-svc (✅)     ├── usage (P4)
  └── alert (P3)          └── alert (P3)
```

---

## Phase 2 — ingestion-service

**Why next:** Fewest dependencies of remaining microservices — only needs Kafka (✅) and device-service (✅), both already running.

### New files in `k8s/charts/microservices-chart/charts/ingestion-service/`
- `Chart.yaml` — name: ingestion-service, version: 0.1.0
- `values.yaml` — port: 8082, image: public.ecr.aws/v6r1m8q2/energy-tracker/ingestion-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-kafka` — `nc -z infra-kafka.default.svc.cluster.local 9092`
    2. `wait-for-device-service` — `wget` on `microservices-device-service.default.svc.cluster.local:8081/actuator/health`
  - Env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `INGESTION_ENDPOINT`, `DEVICE_SERVICE_URL`
  - Spring Actuator readiness/liveness probes (`/actuator/health/readiness`, `/actuator/health/liveness`)
- `templates/service.yaml` — ClusterIP port 8082

### Modified files
- `microservices-chart/Chart.yaml` — add ingestion-service dependency
- `microservices-chart/values.yaml` — add ingestionService block (enabled: true)
- `microservices-chart/templates/configmap-global.yaml` — add:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS=infra-kafka.default.svc.cluster.local:9092`
  - `INGESTION_ENDPOINT=http://microservices-ingestion-service.default.svc.cluster.local:8082/api/v1/ingestion`
  - `DEVICE_SERVICE_URL=http://microservices-device-service.default.svc.cluster.local:8081/api/v1/device`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl get pods -w
kubectl logs deployment/microservices-ingestion-service
# Confirm: "Started IngestionServiceApplication" in logs
```

---

## Phase 3 — alert-service

**Why next:** Depends on Kafka (✅), MySQL (✅), and Mailpit (✅) — all available. No dependency on ingestion or usage services.

### New files in `k8s/charts/microservices-chart/charts/alert-service/`
- `Chart.yaml` — name: alert-service, version: 0.1.0
- `values.yaml` — port: 8084, image: public.ecr.aws/v6r1m8q2/energy-tracker/alert-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-mysql` — `nc -z infra-mysql.default.svc.cluster.local 3306`
    2. `wait-for-kafka` — `nc -z infra-kafka.default.svc.cluster.local 9092`
    3. `wait-for-mailpit` — `nc -z infra-mailpit.default.svc.cluster.local 1025`
  - Env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, MySQL vars from global ConfigMap/Secret
- `templates/service.yaml` — ClusterIP port 8084

### Modified files
- `microservices-chart/Chart.yaml` — add alert-service dependency
- `microservices-chart/values.yaml` — add alertService block (enabled: true)
- `microservices-chart/templates/configmap-global.yaml` — add:
  - `SPRING_MAIL_HOST=infra-mailpit.default.svc.cluster.local`
  - `SPRING_MAIL_PORT=1025`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl get pods -w
kubectl logs deployment/microservices-alert-service
# Confirm: "Started AlertServiceApplication" in logs
```

---

## Phase 4 — usage-service

**Why after Phase 3:** Has the most init container dependencies (4 total). Isolated so startup failures are easier to diagnose.

### New files in `k8s/charts/microservices-chart/charts/usage-service/`
- `Chart.yaml` — name: usage-service, version: 0.1.0
- `values.yaml` — port: 8083, image: public.ecr.aws/v6r1m8q2/energy-tracker/usage-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-kafka` — `nc -z infra-kafka.default.svc.cluster.local 9092`
    2. `wait-for-influxdb` — `nc -z infra-influxdb.default.svc.cluster.local 8086`
    3. `wait-for-user-service` — `wget` on `microservices-user-service.default.svc.cluster.local:8080/actuator/health`
    4. `wait-for-device-service` — `wget` on `microservices-device-service.default.svc.cluster.local:8081/actuator/health`
  - Env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `INFLUX_URL`, `INFLUX_TOKEN` (from secret), `INFLUX_ORG`, `INFLUX_BUCKET`, `DEVICE_SERVICE_URL`, `USER_SERVICE_URL`
- `templates/service.yaml` — ClusterIP port 8083

### Modified files
- `microservices-chart/Chart.yaml` — add usage-service dependency
- `microservices-chart/values.yaml` — add usageService block (enabled: true)
- `microservices-chart/templates/configmap-global.yaml` — add:
  - `INFLUX_URL=http://infra-influxdb.default.svc.cluster.local:8086`
  - `INFLUX_ORG=chieaid24`
  - `INFLUX_BUCKET=usage-bucket`
  - `USER_SERVICE_URL=http://microservices-user-service.default.svc.cluster.local:8080/api/v1/user`
  - `USAGE_SERVICE_URL=http://microservices-usage-service.default.svc.cluster.local:8083/api/v1/usage`
- `microservices-chart/templates/secret-global.yaml` — add `INFLUX_TOKEN=my-token`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl get pods -w
kubectl logs deployment/microservices-usage-service
# Confirm: "Started UsageServiceApplication" in logs
# Test: trigger ingestion → check InfluxDB bucket receives data point
```

---

## Phase 5 — Ollama

**Why last in infra:** Auto-downloads deepseek-r1 (~4–7 GB) on first start. Needs a PersistentVolumeClaim so the model survives pod restarts. Probes need generous `initialDelaySeconds`. Isolated to avoid blocking all other work on a slow download.

### New files in `k8s/charts/infra-chart/charts/ollama/`
- `Chart.yaml` — name: ollama, version: 0.1.0
- `values.yaml` — image: ollama/ollama:latest, port: 11434, model: deepseek-r1, persistence: true, storageSize: 10Gi
- `templates/statefulset.yaml`:
  - Command (mirrors docker-compose): `ollama serve &` → wait for ready → `ollama pull deepseek-r1` → `wait`
  - PVC volumeMount at `/root/.ollama`
  - Probes: `initialDelaySeconds: 120`, `periodSeconds: 15`
- `templates/pvc.yaml` — PersistentVolumeClaim using Minikube's default StorageClass
- `templates/service.yaml` — ClusterIP port 11434, named `infra-ollama`

### Modified files
- `infra-chart/Chart.yaml` — add ollama dependency
- `infra-chart/values.yaml` — add ollama block (enabled: true)

### Verification
```bash
helm upgrade infra ./k8s/charts/infra-chart
kubectl get pods -w       # wait for infra-ollama-0 Running (may take 5-10 min first time)
kubectl logs statefulset/infra-ollama -f
# Confirm: "deepseek-r1" model pulled successfully
```

---

## Phase 6 — insight-service

**Why last:** Depends on Ollama (Phase 5) and usage-service (Phase 4) — both must be running first.

### New files in `k8s/charts/microservices-chart/charts/insight-service/`
- `Chart.yaml` — name: insight-service, version: 0.1.0
- `values.yaml` — port: 8085, image: public.ecr.aws/v6r1m8q2/energy-tracker/insight-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-ollama` — `wget` on `infra-ollama.default.svc.cluster.local:11434/api/tags`
    2. `wait-for-usage-service` — `wget` on `microservices-usage-service.default.svc.cluster.local:8083/actuator/health`
  - Env: `SPRING_AI_OLLAMA_BASE_URL`, `USAGE_SERVICE_URL`
- `templates/service.yaml` — ClusterIP port 8085

### Modified files
- `microservices-chart/Chart.yaml` — add insight-service dependency
- `microservices-chart/values.yaml` — add insightService block (enabled: true)
- `microservices-chart/templates/configmap-global.yaml` — add:
  - `SPRING_AI_OLLAMA_BASE_URL=http://infra-ollama.default.svc.cluster.local:11434`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl get pods -w
kubectl logs deployment/microservices-insight-service
# Confirm: "Started InsightServiceApplication" in logs
```

---

## Complete File Inventory

### Already done
| Phase | Files |
|---|---|
| ✅ 1 (original) | `infra-chart/charts/kafka/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/service.yaml |
| ✅ 1 (original) | `infra-chart/charts/influxdb/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/secret.yaml, templates/service.yaml |
| ✅ 1 (original) | `infra-chart/charts/mailpit/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| ✅ 1 (original) | `infra-chart/charts/kafka-ui/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |

### Remaining files to create
| Phase | Files |
|---|---|
| 2 | `microservices-chart/charts/ingestion-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 3 | `microservices-chart/charts/alert-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 4 | `microservices-chart/charts/usage-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 5 | `infra-chart/charts/ollama/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/pvc.yaml, templates/service.yaml |
| 6 | `microservices-chart/charts/insight-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |

### Files to modify (per phase)
| Phase | File | Change |
|---|---|---|
| 2 | `microservices-chart/Chart.yaml` | Add ingestion-service dependency |
| 2 | `microservices-chart/values.yaml` | Add ingestionService block |
| 2 | `microservices-chart/templates/configmap-global.yaml` | Add Kafka bootstrap, ingestion endpoint, device URL |
| 3 | `microservices-chart/Chart.yaml` | Add alert-service dependency |
| 3 | `microservices-chart/values.yaml` | Add alertService block |
| 3 | `microservices-chart/templates/configmap-global.yaml` | Add mail host/port |
| 4 | `microservices-chart/Chart.yaml` | Add usage-service dependency |
| 4 | `microservices-chart/values.yaml` | Add usageService block |
| 4 | `microservices-chart/templates/configmap-global.yaml` | Add InfluxDB config, service URLs |
| 4 | `microservices-chart/templates/secret-global.yaml` | Add INFLUX_TOKEN |
| 5 | `infra-chart/Chart.yaml` | Add ollama dependency |
| 5 | `infra-chart/values.yaml` | Add ollama block |
| 6 | `microservices-chart/Chart.yaml` | Add insight-service dependency |
| 6 | `microservices-chart/values.yaml` | Add insightService block |
| 6 | `microservices-chart/templates/configmap-global.yaml` | Add Ollama base URL |

---

## Key DNS Names Reference

| Service | Internal DNS | Port |
|---|---|---|
| Kafka | `infra-kafka.default.svc.cluster.local` | 9092 |
| InfluxDB | `infra-influxdb.default.svc.cluster.local` | 8086 |
| Mailpit SMTP | `infra-mailpit.default.svc.cluster.local` | 1025 |
| Ollama | `infra-ollama.default.svc.cluster.local` | 11434 |
| ingestion-service | `microservices-ingestion-service.default.svc.cluster.local` | 8082 |
| usage-service | `microservices-usage-service.default.svc.cluster.local` | 8083 |
| alert-service | `microservices-alert-service.default.svc.cluster.local` | 8084 |
| insight-service | `microservices-insight-service.default.svc.cluster.local` | 8085 |
