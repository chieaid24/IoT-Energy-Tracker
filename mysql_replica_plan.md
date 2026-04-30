# MySQL Read Replicas — Plan

## Context

The IoT Energy Tracker runs a single MySQL 8.3.0 instance (Docker Compose) / single-replica StatefulSet (Helm) shared by `user-service`, `device-service`, and `alert-service`. The user wants to add **one read replica** in each environment, with Spring Boot apps **routing reads to the replica via app-side `@Transactional(readOnly=true)`**. The driver is to demonstrate horizontal read scaling with the smallest reasonable diff to the current setup.

### Bitnami status (per user request to verify)
Confirmed via web search: Broadcom moved free Bitnami Helm charts and `docker.io/bitnami/*` images to a "Bitnami Legacy" repo on **2025-09-29** with no further updates or security patches. **This project is already safe** — the existing Helm chart at `k8s/charts/infra-chart/charts/mysql/` is a hand-rolled chart using the official `mysql:8.3.0` upstream image, not Bitnami. No migration is needed; we extend the existing chart.

### Architecture

```
                 ┌──────────────┐  CHANGE REPLICATION SOURCE TO …
                 │   primary    │◀────────────────────────────┐
  writes  ──▶    │ (server-id=1)│                             │
                 │  log-bin ON  │──── async row-based GTID ──▶│
                 └──────────────┘                             ▼
                                                       ┌──────────────┐
                          reads  ──────────────────▶   │   replica    │
                                                       │ (server-id=2)│
                                                       │ super_read_  │
                                                       │   only=ON    │
                                                       └──────────────┘
```

App-side: `LazyConnectionDataSourceProxy → AbstractRoutingDataSource → {primary HikariCP, replica HikariCP}`, key = `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`.

---

## Phase 0 — Per-service Spring config (do this first; deploy the infra in Phase 1/2 against unchanged apps, then ship the app changes)

Order matters: stand the replica up first. Apps keep working unchanged because the new env vars are optional; only after replication is verified do we ship the routing code.

---

## Phase 1 — Docker Compose: add replica + replication

### 1.1 Source MySQL config (existing `mysql` container)

Files to add/edit:
- **NEW** `docker/mysql/source.cnf` — server-id, log-bin, GTID:
  ```ini
  [mysqld]
  server-id=1
  log-bin=mysql-bin
  binlog_format=ROW
  gtid_mode=ON
  enforce_gtid_consistency=ON
  ```
- **EDIT** `docker/mysql/init.sql` — append a `replicator` user with `REPLICATION SLAVE` privilege:
  ```sql
  CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED WITH mysql_native_password BY 'replicator-pass';
  GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
  FLUSH PRIVILEGES;
  ```
- **EDIT** `docker-compose.yml` `mysql` service — mount the cnf and bump healthcheck to confirm bin-log:
  ```yaml
  volumes:
    - ./docker/mysql/source.cnf:/etc/mysql/conf.d/source.cnf:ro
    - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    - db-data:/var/lib/mysql
  ```

### 1.2 Replica MySQL container

Files to add:
- **NEW** `docker/mysql/replica.cnf`:
  ```ini
  [mysqld]
  server-id=2
  relay-log=mysql-relay-bin
  read_only=ON
  super_read_only=ON
  gtid_mode=ON
  enforce_gtid_consistency=ON
  ```
- **NEW** `docker/mysql/replica-init.sql` — runs on replica's first start, after MySQL is up:
  ```sql
  CHANGE REPLICATION SOURCE TO
    SOURCE_HOST='mysql',
    SOURCE_PORT=3306,
    SOURCE_USER='replicator',
    SOURCE_PASSWORD='replicator-pass',
    SOURCE_AUTO_POSITION=1,
    GET_SOURCE_PUBLIC_KEY=1;
  START REPLICA;
  ```
- **EDIT** `docker-compose.yml` — add `mysql-replica` service depending on `mysql` healthcheck:
  ```yaml
  mysql-replica:
    image: mysql:8.3.0
    container_name: mysql-replica
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: password
    volumes:
      - ./docker/mysql/replica.cnf:/etc/mysql/conf.d/replica.cnf:ro
      - ./docker/mysql/replica-init.sql:/docker-entrypoint-initdb.d/replica-init.sql:ro
      - db-data-replica:/var/lib/mysql
    ports:
      - "3308:3306"
    networks: [iot-network]
    depends_on:
      mysql: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-ppassword"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 60s
  ```
  Add `db-data-replica:` to the `volumes:` section.

### 1.3 Validation — Phase 1
```bash
docker compose down -v && docker compose up -d mysql mysql-replica
docker compose exec mysql mysql -uroot -ppassword -e "SHOW MASTER STATUS\G SHOW BINARY LOGS;"
docker compose exec mysql-replica mysql -uroot -ppassword -e "SHOW REPLICA STATUS\G"
# Expect: Replica_IO_Running=Yes, Replica_SQL_Running=Yes, Last_Error empty, Seconds_Behind_Source=0
docker compose exec mysql mysql -uroot -ppassword energy_tracker -e \
  "CREATE TABLE _probe(id INT); INSERT INTO _probe VALUES(42);"
sleep 2
docker compose exec mysql-replica mysql -uroot -ppassword energy_tracker -e \
  "SELECT * FROM _probe;"   # must print 42
docker compose exec mysql mysql -uroot -ppassword energy_tracker -e "DROP TABLE _probe;"
```
Spin up the rest of the stack against the unchanged app code and confirm services still come up healthy:
```bash
docker compose up -d
curl -fsS localhost:8080/actuator/health  # user-service
curl -fsS localhost:8081/actuator/health  # device-service
curl -fsS localhost:8084/actuator/health  # alert-service
```

---

## Phase 2 — Kubernetes Helm: extend infra-chart with a replica StatefulSet

Two StatefulSets (primary + replica) is preferable to one parameterized StatefulSet with ordinal-based init logic — fewer template branches, cleaner mental model, fits "fewest changes" since the existing StatefulSet stays largely intact as the primary.

### 2.1 Primary StatefulSet — minimal additions

Files:
- **EDIT** `k8s/charts/infra-chart/charts/mysql/templates/configmap.yaml` — add two keys:
  ```yaml
  source.cnf: |
    [mysqld]
    server-id=1
    log-bin=mysql-bin
    binlog_format=ROW
    gtid_mode=ON
    enforce_gtid_consistency=ON
  init.sql: |
    CREATE DATABASE IF NOT EXISTS energy_tracker;
    CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED WITH mysql_native_password BY 'replicator-pass';
    GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
    FLUSH PRIVILEGES;
  ```
- **EDIT** `k8s/charts/infra-chart/charts/mysql/templates/statefulset.yaml` — mount `source.cnf` at `/etc/mysql/conf.d/source.cnf` (subPath); the existing init.sql mount continues to work.
- **EDIT** `k8s/charts/infra-chart/charts/mysql/templates/secret.yaml` — add `replicator-password: replicator-pass`. Surface via `MYSQL_REPLICATION_PASSWORD` env on the replica deployment.

### 2.2 Replica StatefulSet (new subchart files)

New files under `k8s/charts/infra-chart/charts/mysql/templates/`:
- `statefulset-replica.yaml` — separate StatefulSet `{{ .Release.Name }}-mysql-replica`, replicas=1, mounts `replica.cnf` from the same ConfigMap (extend ConfigMap with `replica.cnf` key) and a new `replica-init.sql` key whose `SOURCE_HOST` is `{{ .Release.Name }}-mysql.default.svc.cluster.local`.
- `service-replica.yaml` — ClusterIP service `{{ .Release.Name }}-mysql-replica` on port 3306 selecting the replica pods. Do **not** expose the replica via LoadBalancer (no need for external read access).
- The existing primary service (`{{ .Release.Name }}-mysql`, LoadBalancer:3307) is unchanged.

`values.yaml` additions on the subchart:
```yaml
replica:
  enabled: true
  replicaCount: 1
  service:
    type: ClusterIP
    port: 3306
auth:
  replicationPassword: replicator-pass
```
The parent `infra-chart/values.yaml` mirrors `replica.enabled: true` so it can be toggled.

### 2.3 Validation — Phase 2
```bash
helm upgrade infra ./k8s/charts/infra-chart
kubectl rollout status statefulset/infra-mysql
kubectl rollout status statefulset/infra-mysql-replica

kubectl exec infra-mysql-0 -- mysql -uroot -ppassword \
  -e "SHOW MASTER STATUS\G SELECT user FROM mysql.user WHERE user='replicator';"
kubectl exec infra-mysql-replica-0 -- mysql -uroot -ppassword \
  -e "SHOW REPLICA STATUS\G"   # Replica_IO_Running=Yes, Replica_SQL_Running=Yes

# round-trip
kubectl exec infra-mysql-0 -- mysql -uroot -ppassword energy_tracker \
  -e "CREATE TABLE _probe(id INT); INSERT INTO _probe VALUES(99);"
sleep 2
kubectl exec infra-mysql-replica-0 -- mysql -uroot -ppassword energy_tracker \
  -e "SELECT * FROM _probe;"   # must print 99
kubectl exec infra-mysql-0 -- mysql -uroot -ppassword energy_tracker \
  -e "DROP TABLE _probe;"

# microservices still healthy with no app changes yet
kubectl rollout restart deployment microservices-user-service microservices-device-service microservices-alert-service
kubectl rollout status deployment microservices-user-service
curl -fsS localhost/api/v1/user/actuator/health
```

---

## Phase 3 — App-side read routing (user-service, device-service, alert-service)

Per-service, identical pattern. Boot 4.0.1 + HikariCP + Spring Data JPA. **No new Maven dependencies** — `spring-jdbc` (already transitive via JPA) provides `AbstractRoutingDataSource` and `LazyConnectionDataSourceProxy`.

### 3.1 Routing infrastructure (each of the 3 services)

New files (paths shown for `user-service`; mirror in `device-service` and `alert-service`):
- `services/user-service/src/main/java/com/chieaid24/user_service/config/RoutingDataSource.java` — extends `AbstractRoutingDataSource`, `determineCurrentLookupKey()` returns `"REPLICA"` when `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` is true, else `"PRIMARY"`. Log the resolved key at DEBUG.
- `services/user-service/src/main/java/com/chieaid24/user_service/config/DataSourceConfig.java`:
  - `@ConfigurationProperties("spring.datasource")` → primary `DataSourceProperties`, build via `DataSourceBuilder.create().type(HikariDataSource.class)` with `maximumPoolSize=8`, register a Hikari `MeterBinder` named `primary`.
  - `@ConfigurationProperties("spring.datasource.replica")` → replica `DataSourceProperties` (URL/username/password); fall back to primary's username/password when the replica-specific values are blank. Build same way with `maximumPoolSize=4`, MeterBinder name `replica`.
  - Build `RoutingDataSource` with `targetDataSources={PRIMARY:primary, REPLICA:replica}`, `defaultTargetDataSource=primary`.
  - Wrap in `LazyConnectionDataSourceProxy` and mark **only that bean** `@Primary`.
- (user-service only) `@FlywayDataSource`-annotated bean returning the primary `HikariDataSource` directly. Hard requirement — Flyway must never see the routing proxy.

### 3.2 Service-method `@Transactional(readOnly=true)` annotations

Apply **only on safe read paths**. `SimpleJpaRepository` already wraps repository-method calls in `readOnly=true`, but routing decisions should be explicit at the service layer where humans review them.

- **user-service** — DO **NOT** annotate `AuthService.login` / `googleLogin` (read-after-register hazard). Leave the rest of `UserService` reads as today (repo defaults still route them to replica via auto `readOnly=true`). Do not wrap multi-step service methods.
- **device-service** — annotate `getDeviceById`, `getAllDevicesByUserId`, `getTotalDevices` with `@Transactional(readOnly=true)`.
- **alert-service** — annotate **only the controller-driven read methods** (e.g. `findByUserIdOrderByCreatedAtDesc`, `countByUserId`). Do **not** annotate the Kafka listener path (`AlertService.energyUsageAlertEvent`) — `readOnly=true` flips Hibernate to `FlushMode.MANUAL` and silently no-ops accidental writes.

### 3.3 Env var wiring

Docker Compose `docker-compose.yml` — add to each of the 3 services:
```yaml
SPRING_DATASOURCE_REPLICA_URL: jdbc:mysql://mysql-replica:3306/energy_tracker
SPRING_DATASOURCE_REPLICA_USERNAME: root        # optional, defaults to primary
SPRING_DATASOURCE_REPLICA_PASSWORD: password    # optional, defaults to primary
```

Kubernetes `k8s/charts/microservices-chart/templates/configmap-global.yaml`:
```
SPRING_DATASOURCE_REPLICA_URL: jdbc:mysql://infra-mysql-replica.default.svc.cluster.local:3306/energy_tracker
```
Username/password reuse the existing global config + secret (no new keys needed).

### 3.4 Validation — Phase 3 (per service)

After **each** of the three services is rebuilt and redeployed:

1. **Build hygiene** — per service:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
   mvn spotless:apply -f services/<svc>/pom.xml && mvn -q compile -f services/<svc>/pom.xml
   ```
2. **Boot smoke** — `/actuator/health` returns 200; no `Failed to determine driver` errors; logs show two HikariCP pools (`primary`, `replica`) initialized.
3. **Wire-level proof reads went to the replica** (preferred over `general_log` — uses default-on `performance_schema`):
   ```bash
   # Hit a read endpoint
   curl -fsS localhost:8081/api/v1/devices/by-user/1
   # On primary
   docker compose exec mysql mysql -uroot -ppassword -e \
     "SELECT SQL_TEXT FROM performance_schema.events_statements_history_long
      WHERE SQL_TEXT LIKE '%FROM device%' ORDER BY TIMER_START DESC LIMIT 5;"
   # On replica
   docker compose exec mysql-replica mysql -uroot -ppassword -e \
     "SELECT SQL_TEXT FROM performance_schema.events_statements_history_long
      WHERE SQL_TEXT LIKE '%FROM device%' ORDER BY TIMER_START DESC LIMIT 5;"
   # Expect: SELECT shows up on REPLICA, not on PRIMARY
   ```
4. **Wire-level proof writes went to the primary**:
   ```bash
   curl -X POST localhost:8081/api/v1/devices -H 'content-type: application/json' \
     -d '{"name":"test","type":"X","location":"Y","userId":1}'
   # INSERT must appear on PRIMARY, not REPLICA
   ```
5. **App-side proof** — DEBUG-level log line from `RoutingDataSource.determineCurrentLookupKey` confirms the routing key chosen for each request matches the wire-level evidence.
6. **Flyway sanity** (user-service) — restart `microservices-user-service`, confirm Flyway logs show migrations applied against the **primary** URL.

---

## Phase 4 — Final cross-cutting checks

- `kubectl exec infra-mysql-replica-0 -- mysql -uroot -ppassword -e "SELECT @@global.read_only, @@global.super_read_only;"` returns `1, 1` — replica rejects writes at the engine level.
- Stop the replica (`docker compose stop mysql-replica` / `kubectl scale --replicas=0 statefulset/infra-mysql-replica`) and verify writes still succeed and reads degrade gracefully (HikariCP fails fast on the replica pool, `LazyConnectionDataSourceProxy` does **not** mask this — calls under `readOnly=true` will error). Document this as a known limitation; out-of-scope to add fallback-to-primary behavior.
- Confirm Hikari metrics emitted as `hikaricp_connections_active{pool="primary"}` and `{pool="replica"}` separately in Prometheus.

---

## Known Limitations (call out in the PR description)

1. **Replication lag** — async GTID replication; a write followed by an immediate read on the replica can return stale data (sub-second on healthy LAN). `AuthService.login` is intentionally left routing to primary to avoid the user-visible "register then immediately log in" failure.
2. **Replica down** = read endpoints fail. We don't auto-fall-back to primary because that masks the outage and breaks the "scale reads" premise. Operator response is to scale the replica back up.
3. **`@Transactional(readOnly=true)` is a footgun on Kafka listeners** — silently no-ops accidental writes via Hibernate `FlushMode.MANUAL`. Annotate only verified-read paths.
4. **DataSourceHealthIndicator only checks the primary** (the `@Primary` lazy proxy resolves to primary when no transaction is active). A dead replica will not flip pod readiness. Add a custom indicator in a future PR if desired.

## Critical Files

**Docker Compose (Phase 1)**
- `docker-compose.yml` — add `mysql-replica` service, mount cnf into `mysql`
- `docker/mysql/init.sql` — extend with replicator user
- `docker/mysql/source.cnf` (NEW), `docker/mysql/replica.cnf` (NEW), `docker/mysql/replica-init.sql` (NEW)

**Helm (Phase 2)**
- `k8s/charts/infra-chart/charts/mysql/templates/statefulset.yaml` — mount `source.cnf`
- `k8s/charts/infra-chart/charts/mysql/templates/configmap.yaml` — add `source.cnf`, `replica.cnf`, `replica-init.sql`
- `k8s/charts/infra-chart/charts/mysql/templates/secret.yaml` — add `replicator-password`
- `k8s/charts/infra-chart/charts/mysql/templates/statefulset-replica.yaml` (NEW)
- `k8s/charts/infra-chart/charts/mysql/templates/service-replica.yaml` (NEW)
- `k8s/charts/infra-chart/charts/mysql/values.yaml` + `k8s/charts/infra-chart/values.yaml` — `replica.enabled` block
- `k8s/charts/microservices-chart/templates/configmap-global.yaml` — `SPRING_DATASOURCE_REPLICA_URL`

**App-side routing (Phase 3)**
- `services/user-service/src/main/java/com/chieaid24/user_service/config/{DataSourceConfig,RoutingDataSource}.java` (NEW)
- `services/device-service/src/main/java/com/chieaid24/device_service/config/{DataSourceConfig,RoutingDataSource}.java` (NEW)
- `services/alert-service/src/main/java/com/chieaid24/alert_service/config/{DataSourceConfig,RoutingDataSource}.java` (NEW)
- `services/device-service/.../service/DeviceService.java` — add `@Transactional(readOnly=true)` to read methods
- `services/alert-service/.../service/AlertService.java` — add `@Transactional(readOnly=true)` only on controller-side reads, **not** on the Kafka listener
- `docker-compose.yml` — three new env vars on the three services

## Sources

- [Bitnami Deprecation Notice 2025 — Chkk](https://www.chkk.io/blog/bitnami-deprecation)
- [Bitnami legacy migration — Chainguard](https://www.chainguard.dev/supply-chain-security-101/a-practical-guide-to-migrating-helm-charts-from-bitnami)
- [MySQL replica setup with docker-compose (2025)](https://victoronsoftware.com/posts/mysql-master-slave-replication/)
- [Run a Replicated Stateful Application — Kubernetes docs](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/)
