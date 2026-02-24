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
| Kafka | ❌ not yet |
| InfluxDB | ❌ not yet |
| Mailpit | ❌ not yet |
| Ollama | ❌ not yet |
| Kafka UI | ❌ not yet |
| ingestion-service | ❌ not yet |
| alert-service | ❌ not yet |
| usage-service | ❌ not yet |
| insight-service | ❌ not yet |

---

## Dependency Graph

```
MySQL (✅ done)         Kafka (P1)          InfluxDB (P1)    Mailpit (P1)    Ollama (P4)
  ├── user-svc (✅)       ├── ingestion (P2)    └── usage (P3)    └── alert (P2)    └── insight (P4)
  ├── device-svc (✅)     ├── usage (P3)
  └── alert (P2)          └── alert (P2)
```

---

## Phase 1 — Complete Infrastructure (except Ollama)

**Why first:** All remaining microservices depend on Kafka. InfluxDB and Mailpit complete the infra tier so microservices can be added in one shot. Ollama is deferred to Phase 4 due to its multi-GB model download.

### New files in `k8s/charts/infra-chart/charts/`

#### `kafka/`
- `Chart.yaml` — name: kafka, version: 0.1.0
- `values.yaml` — image: apache/kafka:latest, nodeId: 1, clusterId: energy-tracker-cluster-1, resources: 512Mi/1Gi, persistence: false
- `templates/statefulset.yaml` — **Must be a StatefulSet** (not Deployment) so the pod gets a stable hostname for KRaft quorum. Use emptyDir for data volume.
  - Critical env vars:
    - `KAFKA_CONTROLLER_QUORUM_VOTERS=1@infra-kafka-0.infra-kafka-headless.default.svc.cluster.local:9093`
    - `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://infra-kafka.default.svc.cluster.local:9092,EXTERNAL://localhost:9094`
    - `KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,EXTERNAL://0.0.0.0:9094,CONTROLLER://0.0.0.0:9093`
    - All other KRaft env vars match docker-compose exactly
  - tcpSocket readiness/liveness probes on port 9092
- `templates/service.yaml` — Two services (same pattern as existing mysql):
  - `infra-kafka-headless` (clusterIP: None) — required for StatefulSet pod DNS
  - `infra-kafka` (ClusterIP, port 9092) — what microservices connect to

#### `influxdb/`
- `Chart.yaml` — name: influxdb, version: 0.1.0
- `values.yaml` — image: influxdb:2.7, org: chieaid24, bucket: usage-bucket, token: my-token, retention: 1w, persistence: false
- `templates/statefulset.yaml` — StatefulSet with emptyDir, all `DOCKER_INFLUXDB_INIT_*` env vars from docker-compose, token from secret
- `templates/secret.yaml` — stores admin token (my-token)
- `templates/service.yaml` — ClusterIP on port 8086, named `infra-influxdb`

#### `mailpit/`
- `Chart.yaml` — name: mailpit, version: 0.1.0
- `values.yaml` — image: axllent/mailpit:latest, smtpPort: 1025, webPort: 8025
- `templates/deployment.yaml` — simple Deployment (stateless), no init containers needed
- `templates/service.yaml` — ClusterIP exposing ports 1025 (smtp) and 8025 (web UI)

#### `kafka-ui/`
- `Chart.yaml` — name: kafka-ui, version: 0.1.0
- `values.yaml` — image: ghcr.io/kafbat/kafka-ui:latest, port: 8080
- `templates/deployment.yaml` — env: `KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=infra-kafka.default.svc.cluster.local:9092`
- `templates/service.yaml` — ClusterIP port 8080

### Modified files
- `infra-chart/Chart.yaml` — add kafka, influxdb, mailpit, kafka-ui as dependencies with `condition:` toggles
- `infra-chart/values.yaml` — add enabled blocks for each (enabled: true); uncomment existing kafka/influxdb placeholders

### Verification
```bash
helm upgrade infra ./k8s/charts/infra-chart
kubectl get pods -w
# Expect Running: infra-kafka-0, infra-influxdb-0, infra-mailpit-*, infra-kafka-ui-*
kubectl exec -it infra-kafka-0 -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## Phase 2 — ingestion-service and alert-service

**Why these two:** ingestion-service only needs Kafka + device-service (both available after Phase 1). alert-service needs Kafka + MySQL + Mailpit — all available. They have the fewest unresolved dependencies of the remaining microservices.

### New files in `k8s/charts/microservices-chart/charts/`

#### `ingestion-service/`
- `Chart.yaml`, `values.yaml` — port: 8082, image: public.ecr.aws/v6r1m8q2/energy-tracker/ingestion-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-kafka` — `nc -z infra-kafka.default.svc.cluster.local 9092`
    2. `wait-for-device-service` — `wget` on `microservices-device-service.default.svc.cluster.local:8081/actuator/health`
  - Env (from global ConfigMap + additions): `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `INGESTION_ENDPOINT`, `DEVICE_SERVICE_URL`
  - Spring Actuator readiness/liveness probes
- `templates/service.yaml` — ClusterIP port 8082

#### `alert-service/`
- `Chart.yaml`, `values.yaml` — port: 8084, image: public.ecr.aws/v6r1m8q2/energy-tracker/alert-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers:
    1. `wait-for-mysql` — `nc -z infra-mysql.default.svc.cluster.local 3306`
    2. `wait-for-kafka` — `nc -z infra-kafka.default.svc.cluster.local 9092`
    3. `wait-for-mailpit` — `nc -z infra-mailpit.default.svc.cluster.local 1025`
  - Env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_MAIL_HOST=infra-mailpit.default.svc.cluster.local`, `SPRING_MAIL_PORT=1025`, MySQL vars from global ConfigMap/Secret
- `templates/service.yaml` — ClusterIP port 8084

### Modified files
- `microservices-chart/Chart.yaml` — add ingestion-service, alert-service dependencies
- `microservices-chart/values.yaml` — add ingestionService, alertService blocks (enabled: true)
- `microservices-chart/templates/configmap-global.yaml` — add `SPRING_KAFKA_BOOTSTRAP_SERVERS=infra-kafka.default.svc.cluster.local:9092`, `INGESTION_ENDPOINT`, `DEVICE_SERVICE_URL`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl get pods -w
kubectl logs deployment/microservices-ingestion-service
kubectl logs deployment/microservices-alert-service
```

---

## Phase 3 — usage-service

**Why separate phase:** usage-service has 4 init container dependencies (Kafka, InfluxDB, user-service, device-service). Isolating it in its own phase makes startup failures easier to diagnose.

### New files in `k8s/charts/microservices-chart/charts/`

#### `usage-service/`
- `Chart.yaml`, `values.yaml` — port: 8083, image: public.ecr.aws/v6r1m8q2/energy-tracker/usage-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers (busybox:1.36):
    1. `wait-for-kafka` — nc on infra-kafka:9092
    2. `wait-for-influxdb` — nc on infra-influxdb:8086
    3. `wait-for-user-service` — wget on microservices-user-service:8080/actuator/health
    4. `wait-for-device-service` — wget on microservices-device-service:8081/actuator/health
  - Env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `INFLUX_URL=http://infra-influxdb.default.svc.cluster.local:8086`, `INFLUX_TOKEN` (from secret), `INFLUX_ORG=chieaid24`, `INFLUX_BUCKET=usage-bucket`, `DEVICE_SERVICE_URL`, `USER_SERVICE_URL`
- `templates/service.yaml` — ClusterIP port 8083

### Modified files
- `microservices-chart/Chart.yaml` — add usage-service dependency
- `microservices-chart/values.yaml` — add usageService block
- `microservices-chart/templates/configmap-global.yaml` — add `INFLUX_URL`, `INFLUX_ORG`, `INFLUX_BUCKET`, `USAGE_SERVICE_URL`
- `microservices-chart/templates/secret-global.yaml` — add `INFLUX_TOKEN=my-token`

### Verification
```bash
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl logs deployment/microservices-usage-service
# Test: trigger ingestion → check InfluxDB bucket receives data point
```

---

## Phase 4 — Ollama and insight-service

**Why last:** Ollama auto-downloads the deepseek-r1 model (~4–7 GB) on first start. It needs a PersistentVolumeClaim so the model survives pod restarts and doesn't re-download every time. Probes need generous `initialDelaySeconds` (120s+). Isolating this last prevents it from blocking all other work.

### New files in `k8s/charts/infra-chart/charts/ollama/`
- `Chart.yaml` — name: ollama, version: 0.1.0
- `values.yaml` — image: ollama/ollama:latest, port: 11434, model: deepseek-r1, persistence: true, storageSize: 10Gi
- `templates/statefulset.yaml`:
  - Entrypoint script (mirrors docker-compose): `ollama serve &` → wait for ready → `ollama pull deepseek-r1` → `wait`
  - PVC volumeMount at `/root/.ollama`
  - Probes: `initialDelaySeconds: 120`, `periodSeconds: 15` (model download is slow)
- `templates/pvc.yaml` — PersistentVolumeClaim using Minikube's default StorageClass
- `templates/service.yaml` — ClusterIP port 11434, named `infra-ollama`

### New files in `k8s/charts/microservices-chart/charts/insight-service/`
- `Chart.yaml`, `values.yaml` — port: 8085, image: public.ecr.aws/v6r1m8q2/energy-tracker/insight-service:latest, replicas: 1
- `templates/deployment.yaml`:
  - Init containers:
    1. `wait-for-ollama` — wget on `infra-ollama.default.svc.cluster.local:11434/api/tags`
    2. `wait-for-usage-service` — wget on `microservices-usage-service.default.svc.cluster.local:8083/actuator/health`
  - Env: `SPRING_AI_OLLAMA_BASE_URL=http://infra-ollama.default.svc.cluster.local:11434`, `USAGE_SERVICE_URL`
- `templates/service.yaml` — ClusterIP port 8085

### Modified files
- `infra-chart/Chart.yaml` + `values.yaml` — add ollama dependency (enabled: true)
- `microservices-chart/Chart.yaml` + `values.yaml` — add insight-service dependency
- `microservices-chart/templates/configmap-global.yaml` — add `SPRING_AI_OLLAMA_BASE_URL`, `USAGE_SERVICE_URL`

### Verification
```bash
helm upgrade infra ./k8s/charts/infra-chart
kubectl get pods -w       # wait for infra-ollama-0 Running (may take 5-10 min first time)
kubectl logs statefulset/infra-ollama   # confirm "deepseek-r1" model pulled
helm upgrade microservices ./k8s/charts/microservices-chart
kubectl logs deployment/microservices-insight-service
```

---

## Complete File Inventory

### New files (20 across all phases)
| Phase | Files |
|---|---|
| 1 | `infra-chart/charts/kafka/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/service.yaml |
| 1 | `infra-chart/charts/influxdb/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/secret.yaml, templates/service.yaml |
| 1 | `infra-chart/charts/mailpit/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 1 | `infra-chart/charts/kafka-ui/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 2 | `microservices-chart/charts/ingestion-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 2 | `microservices-chart/charts/alert-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 3 | `microservices-chart/charts/usage-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |
| 4 | `infra-chart/charts/ollama/` — Chart.yaml, values.yaml, templates/statefulset.yaml, templates/pvc.yaml, templates/service.yaml |
| 4 | `microservices-chart/charts/insight-service/` — Chart.yaml, values.yaml, templates/deployment.yaml, templates/service.yaml |

### Modified files (6 total)
| File | Changes |
|---|---|
| `infra-chart/Chart.yaml` | Add kafka, influxdb, mailpit, kafka-ui, ollama as dependencies |
| `infra-chart/values.yaml` | Add/uncomment enabled blocks for each new infra service |
| `microservices-chart/Chart.yaml` | Add ingestion, alert, usage, insight as dependencies |
| `microservices-chart/values.yaml` | Add service blocks for each new microservice |
| `microservices-chart/templates/configmap-global.yaml` | Add Kafka, InfluxDB, Ollama URLs and service URLs |
| `microservices-chart/templates/secret-global.yaml` | Add INFLUX_TOKEN |

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
