# Redis Cache — Kubernetes Implementation Plan

Phases 1–5 (Docker Compose) are complete. This file tracks the remaining Kubernetes phases.
Do NOT start the next phase until ALL validation steps in the current phase pass.

---

## Phase 6 — Add Redis Subchart to infra-chart

### Changes
- [ ] Create `k8s/charts/infra-chart/charts/redis/Chart.yaml`
- [ ] Create `k8s/charts/infra-chart/charts/redis/values.yaml` (image: `redis:7-alpine`, ClusterIP service on port 6379)
- [ ] Create `k8s/charts/infra-chart/charts/redis/templates/deployment.yaml`
- [ ] Create `k8s/charts/infra-chart/charts/redis/templates/service.yaml`
- [ ] Add `redis` dependency and `redis.enabled: true` to infra-chart `Chart.yaml` and `values.yaml`
- [ ] `helm upgrade infra ./k8s/charts/infra-chart`

### Validation
- [ ] `kubectl get pods | grep redis` — Redis pod reaches Running state
- [ ] `kubectl exec -it <redis-pod> -- redis-cli ping` returns `PONG`
- [ ] DNS reachable from within cluster:
  ```bash
  kubectl run redis-test --rm -it --restart=Never --image=redis:7-alpine -- \
    redis-cli -h infra-redis.default.svc.cluster.local ping
  ```
  Expected: `PONG`

---

## Phase 7 — Add Redis Env Vars to Global ConfigMap

### Changes
- [ ] Add `SPRING_DATA_REDIS_HOST: infra-redis.default.svc.cluster.local` to `microservices-chart/templates/configmap-global.yaml`
- [ ] Add `SPRING_DATA_REDIS_PORT: "6379"` to the same ConfigMap
- [ ] `helm upgrade microservices ./k8s/charts/microservices-chart`

### Validation
- [ ] `kubectl get configmap microservices-global-config -o yaml | grep -i redis` — both vars present with correct values
- [ ] `kubectl rollout restart deployment microservices-usage-service`
- [ ] `kubectl rollout status deployment microservices-usage-service` — rollout completes

---

## Phase 8 — Deploy and Validate in Kubernetes

### Changes
- [ ] Rebuild and push usage-service image to ECR:
  ```bash
  docker build -t public.ecr.aws/v6r1m8q2/energy-tracker/usage-service:latest services/usage-service
  docker push public.ecr.aws/v6r1m8q2/energy-tracker/usage-service:latest
  ```
- [ ] Restart usage-service to pull latest image:
  ```bash
  kubectl rollout restart deployment microservices-usage-service
  ```

### Validation
- [ ] `kubectl rollout status deployment microservices-usage-service` — completes successfully
- [ ] `curl http://localhost/api/v1/usage/actuator/health` (via ingress) — Redis component shows `UP`
- [ ] REST cache (via ingress):
  - [ ] First call to `/api/v1/usage/{userId}?days=7` — InfluxDB query log appears in `kubectl logs`
  - [ ] Second identical call — no InfluxDB log, instant response
  - [ ] `kubectl exec -it <redis-pod> -- redis-cli KEYS "*"` — cache entry visible
- [ ] Scheduler lock:
  - [ ] `kubectl logs <usage-service-pod>` — aggregation logs every ~10s
  - [ ] `kubectl exec -it <redis-pod> -- redis-cli KEYS "*"` — `aggregation-lock` key visible post-tick
- [ ] Scale to 2 replicas:
  ```bash
  kubectl scale deployment microservices-usage-service --replicas=2
  ```
  - [ ] Watch both pod logs: only 1 InfluxDB aggregation query per 10s window
  - [ ] Alerts still reach Mailpit (`http://localhost:8025`)
- [ ] Scale back to 1:
  ```bash
  kubectl scale deployment microservices-usage-service --replicas=1
  ```
- [ ] Prometheus: `curl http://localhost:9090/api/v1/targets` — usage-service target still UP
