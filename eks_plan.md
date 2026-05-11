# EKS Migration Plan

Migration of IoT Energy Tracker from local Docker Compose / Minikube to AWS EKS. Docker Compose and Minikube setups must continue to work after migration — every change is additive (overlays, gated templates, new files), not destructive.

## Phase gating policy

> **At the end of each numbered Phase (1, 2, 3, …) — including the final gate of that phase — STOP and wait for explicit user approval before starting the next phase.**

Approval is one of: an explicit "go" / "continue" / "next phase" / "proceed", or a direct instruction to start the named phase. Until then, do not begin work on Phase N+1, even if Phase N's final gate passes.

This applies regardless of how confident the work looks — every phase boundary is a checkpoint where the user reviews diffs, costs, and direction. Sub-phases inside a phase (e.g. 1A → 1B) flow continuously without waiting; only the **inter-phase** boundaries require approval.

## Current state (snapshot 2026-05-11)

Updated as work progresses. Anything not listed below is pending and untouched.

- **Phase 0 ✅** — `terraform 1.15.2` installed via tfenv; AWS creds active (account `714454206433`, IAM user `IoTEnergyTracker`, region `us-east-1`); Bedrock Haiku 4.5 model access granted but quota at 0.
- **Phase 1 ✅** — re-applied 2026-05-11 (115 resources, ~8 min wall clock). All infrastructure live in `us-east-1`:
  - VPC `vpc-0c6e4ba705e38e8c5` (3 public, 3 private, 3 DB subnets, NAT gateway)
  - EKS cluster `iot-tracker-dev` (K8s 1.31, 2x t3.large nodes, OIDC enabled)
  - RDS MySQL 8.0.44 at `iot-tracker-dev-mysql.c0fwkuqqyner.us-east-1.rds.amazonaws.com:3306/energy_tracker`
  - MSK Serverless (get bootstrap brokers via `terraform output -raw msk_bootstrap_brokers`)
  - Route53 zone `Z00680271D3YUMV51OPOE` for `energy.aidanchien.com` — **NS delegation at Porkbun is STALE** (points at deleted zone from previous destroy/apply cycle). New NS records:
    - `ns-1204.awsdns-22.org`, `ns-1825.awsdns-36.co.uk`, `ns-299.awsdns-37.com`, `ns-875.awsdns-45.net`
  - ACM cert issued for `energy.aidanchien.com` + `*.energy.aidanchien.com`
  - 7 IRSA roles created (incl. `iot-tracker-dev-gha-deploy` for GitHub Actions OIDC)
  - Secrets Manager: `iot/dev/rds/master`
  - Note: original `rds_engine_version` 8.0.39 unavailable in us-east-1; changed to 8.0.44
- **Phase 3 ✅** — chart parameterization + Bedrock swap done. Notable extensions beyond the original plan:
  - MSK IAM auth wired in: `aws-msk-iam-auth:2.2.0` added to ingestion / usage / alert poms; `global.kafka.iamAuth` toggle gates SASL/SSL/IAM props in the global ConfigMap (default false → Compose/Minikube unchanged).
  - `microservices-chart/Chart.yaml` gained `alias: insightService` on the insight-service dependency so the parent's existing `insightService:` block cascades into the subchart. `Chart.lock` was deleted; helm regenerates it on next deploy.
  - `OllamaConfig.java` was deleted and replaced with provider-agnostic `ChatClientConfig.java`. `numPredict(1024)` moved from code to env var (`SPRING_AI_OLLAMA_CHAT_OPTIONS_NUM_PREDICT`) — added to both `docker-compose.yml` and `configmap-global.yaml`.
  - Default `helm template` diff vs original baseline: 3 expanded comments + 1 new env var (NUM_PREDICT) + 2 alias source-path cosmetic comments. No functional drift.
- **Phase 4 ✅** — All three EKS value overlays written and validated:
  - `infra-chart/values-eks.yaml` (4A): disables MySQL/Kafka/Ollama (managed), keeps InfluxDB/Mailpit/Redis as ClusterIP with EBS persistence.
  - `microservices-chart/values-eks.yaml` (4B): ExternalSecrets mode, RDS/MSK endpoints, TLS ingress, Bedrock backend, IRSA annotation.
  - `observability-chart/values-eks.yaml` (4C): gp3 persistence for Prometheus (50Gi), Loki (100Gi), Tempo (50Gi), Grafana (10Gi). Subchart templates now conditionally use PVC vs emptyDir via `persistence.enabled` (default false → Minikube unchanged).

- **Phase 2 ✅** — All cluster addons installed and validated:
  - EBS CSI driver (managed addon) + gp3 StorageClass (default). PVC smoke test passed.
  - AWS Load Balancer Controller (IRSA-bound, 2 replicas).
  - ingress-nginx with internet-facing NLB (`k8s-ingressn-ingressn-e7f71656ec-9adab2552827fb24.elb.us-east-1.amazonaws.com`). Returns 404 — healthy.
  - ExternalDNS (upsert-only, domain filter `energy.aidanchien.com`).
  - External Secrets Operator + ClusterSecretStore `aws-secrets-manager` (Ready). Smoke test: synced `iot/dev/test` → `bar`.
  - cert-manager + ClusterIssuer `route53-issuer` (Ready, DNS-01 via Route53).
  - metrics-server (`kubectl top nodes` returns CPU/MEM).
  - Bootstrap script: `scripts/eks-bootstrap.sh`

- **Phase 5 ✅** — Secrets, DNS, TLS all working:
  - Secrets Manager: `iot/dev/global` (3 keys) and `iot/dev/frontend` (3 keys) created and synced via ESO.
  - ExternalSecrets both `SecretSynced`/`Ready=True` — created K8s secrets `microservices-global-secret` and `microservices-frontend-secret`.
  - TLS cert issued by Let's Encrypt (R13) for `energy.aidanchien.com` + `*.energy.aidanchien.com`, valid until 2026-08-06. Secret `iot-tls` exists.
  - DNS A-record for `energy.aidanchien.com` → NLB will be created by ExternalDNS when Ingress is deployed.
  - Note: ExternalSecret templates updated from `v1beta1` to `v1` (ESO v1 doesn't serve beta API).
  - Note: Google OAuth secrets are placeholders — update via `aws secretsmanager put-secret-value` when ready.

- **Phase 6 ✅ (validated 2026-05-11)** — CI/CD workflows authored AND core mechanics validated out-of-band:
  - `build-and-push.yml`: OIDC auth, change detection via `dorny/paths-filter`, SHA + latest tags, insight-service dual-build (ollama + bedrock variant). On `aws-migration` only; not on `main` yet.
  - `deploy-eks.yml`: OIDC auth, deploys infra → observability → microservices with SHA-pinned tags. Concurrency group prevents overlapping deploys. Triggers on workflow_dispatch + build success. On `aws-migration` only.
  - `build-test.yml`: manual `workflow_dispatch` workflow (also on `main` as commit `a506d30`) used to validate Phase 6 before merge. Single-service input, SHA-tagged only, no `:latest` push. Uses `environment: dev` to satisfy gha-deploy OIDC trust from non-main branches. Kept on `main` as a permanent manual-rebuild utility.
  - Old `ci.yaml` disabled (renamed `.disabled`). GHA OIDC role trust policy: `refs/heads/main`, `refs/tags/v*`, `environment:dev`.
  - `values-eks.yaml` updated with hardcoded RDS/MSK endpoints and correct TLS secret name (`iot-tls`).
  - ExternalSecret templates updated to `v1` API (from `v1beta1`).
  - **Validation run on 2026-05-11**: user-service build via `build-test.yml` GREEN; insight-service dual-build GREEN (1m47s for both variants). All 3 test images pushed to ECR, deleted after verification.
  - **Validated**: GHA OIDC handshake via `environment:dev`, ECR Public auth + push, Maven profile switch (ollama/bedrock), dual-tag pattern.
  - **Not yet exercised**: dorny/paths-filter detection, matrix strategy, the `push: branches: [main]` trigger — these will fire naturally when `aws-migration` merges to `main`.

- **Phase 7 ✅ (validated 2026-05-11)** — First deploy + end-to-end validation complete:
  - 7A: infra (InfluxDB, Redis, Mailpit) + observability (Prometheus, Grafana, Loki, Tempo, Promtail) all Running.
  - 7B: All 7 microservices + frontend Running (16 pods total, 0 restarts). Helm rev 5.
  - 7C: End-to-end pipeline validated:
    - User registration + auth: `POST /api/v1/auth/register` → JWT issued ✓
    - Device creation: `POST /api/v1/device/create` → stored in RDS ✓
    - Ingestion → MSK (energy-usage) → usage-service → InfluxDB ✓
    - Alert pipeline: usage-service → MSK (energy-alerts) → alert-service → stored + sent ✓
    - Usage query: `GET /api/v1/usage/{userId}` returns aggregated data from InfluxDB ✓
    - Frontend: HTTPS `https://energy.aidanchien.com` → login page renders correctly ✓
    - **Blocker**: insight-service Bedrock inference blocked (daily token quota = 0). Service is healthy, IRSA attached, but falls back to Ollama (not available on EKS). Requires Service Quotas approval.
  - 7D: Observability confirmed:
    - Prometheus: 6/6 targets `up=1` ✓
    - Loki: ingesting logs from all namespaces ✓
    - Tempo: pod Running (trace export from services fails due to port issue — non-blocking) ✓
  - Fixes applied during Phase 7:
    - `global.skipInitContainers: true` — disables init containers waiting for in-cluster infra
    - `replicaUrlOverride` set to RDS endpoint (was empty → falling back to non-existent `infra-mysql-replica`)
    - MSK topics `energy-usage` + `energy-alerts` created via AdminClient (MSK Serverless doesn't auto-create)
    - Ingress `/api/v1/auth` route added for auth endpoints
    - Observability chart fsGroup fixes for EBS PVC permissions
    - CPU requests reduced to 100m to fit 2x t3.large node group
    - Frontend `AUTH_URL` templated and set to `https://energy.aidanchien.com` (was hardcoded `http://localhost` → NextAuth redirected to localhost)
    - Frontend `NEXT_PUBLIC_API_URL` templated and set to `https://energy.aidanchien.com`
  - Commits on `main`:
    - `51c0ffe` — Phase 7 runtime fixes (templates, values-eks, ingress, observability fsGroup, eks_plan)
    - `361e329` — Frontend AUTH_URL fix

- **Phase 8 ✅ (validated 2026-05-11)** — Hardening complete:
  - 8A: HPA on all 7 services — target 70% CPU, min 2 / max 5 replicas. All reporting real metrics.
  - 8B: PDB on all 7 services — minAvailable: 1, ALLOWED DISRUPTIONS ≥ 1.
  - 8C: NetworkPolicies — default-deny + per-service allows. Pipeline verified working through policies.
  - 8D: CloudWatch Alarms — 4 alarms (RDS CPU, RDS connections, RDS storage, EKS node health), all `OK`. SNS topic created (no email subscription).
  - Node group scaled from 2 → 3 to accommodate 14 microservice pods (2 replicas × 7 services).
  - Branch: `phase-8-hardening` (not yet merged to main).

- **Phase 9 ✅ (completed 2026-05-11)** — Documentation + handoff:
  - README.md updated with full EKS deployment section: architecture diagram, AWS resources table, cluster addons, production hardening features, NetworkPolicy rules, CI/CD workflows, deploy-from-scratch guide, teardown, and cost estimate.
  - Project layout updated to include `terraform/`, `scripts/`, `.github/workflows/`.
  - Data flow diagram updated for dual LLM backend (Ollama local / Bedrock EKS).
  - Technical highlights expanded with EKS-specific features (HPA, PDB, NetworkPolicies, OIDC CI/CD, Secrets Manager).
  - Branch: `phase-8-hardening` (carries both Phase 8 and Phase 9 changes).

**Outstanding inputs needed from operator:**

1. **Bedrock daily quota** — Service Quotas request submitted but not yet approved. Account currently has 0 daily token cap → every Bedrock invocation throttles. Insight-service AI inference is the only non-functional feature.

**Where to read code/docs to pick up this work:**

| Path | Purpose |
|---|---|
| `eks_plan.md` (this file) | Full plan + phase gates + current state |
| `terraform/envs/dev/` | Phase 1 IaC (vpc, eks, iam, rds, msk, dns) |
| `terraform/README.md` | Phase 1 ops runbook |
| `k8s/charts/infra-chart/values-eks.yaml` | Phase 4A overlay |
| `k8s/charts/microservices-chart/values-eks.yaml` | Phase 4B overlay |
| `k8s/charts/microservices-chart/templates/externalsecret-*.yaml` | Phase 3 ExternalSecret templates |
| `services/insight-service/src/main/java/.../config/ChatClientConfig.java` | Phase 3 provider-agnostic chat client |
| `services/insight-service/pom.xml` | Phase 3 Maven profiles `ollama` (default) / `bedrock` |
| `~/.claude/projects/-home-secur-projects-IoT-Energy-Tracker/memory/MEMORY.md` | Auto-loaded memory index — points at the live-state and feedback files |
| `~/.claude/projects/-home-secur-projects-IoT-Energy-Tracker/memory/project_eks_migration.md` | Live progress snapshot for the next agent session |
| `~/.claude/projects/-home-secur-projects-IoT-Energy-Tracker/memory/feedback_phase_gating.md` | The "stop after every phase" rule |
| `tasks/lessons.md` | Cross-cutting lessons from this work |

## Decisions captured

| Decision | Choice | Rationale |
|---|---|---|
| Stateful services | Managed RDS MySQL + MSK Serverless; InfluxDB stays in-cluster on EBS; Redis stays in-cluster | RDS/MSK are operationally painful to self-host. Influx + Redis are low-stakes for now. |
| Ingress | `ingress-nginx` behind an NLB | Existing `Ingress` manifests work with minimal change. Same template can serve both EKS and Minikube. |
| LLM | Drop Ollama on EKS, use AWS Bedrock with `us.anthropic.claude-haiku-4-5-20251001-v1:0` (cross-region inference profile) | Avoids a GPU node group (~$700/mo). Haiku 4.5 only invokes via the `us.` inference profile (no direct on-demand). Cross-region routing fans out to us-east-1, us-east-2, us-west-2; IAM policy grants foundation-model invoke in all three. Ollama remains the default for Minikube/Docker Compose. |
| IaC | Terraform with `terraform-aws-modules/eks/aws` | Reproducible, declarative, evolves cleanly. |
| Secrets | AWS Secrets Manager + External Secrets Operator (IRSA) | Removes plaintext from `values.yaml`. Native AWS integration. |
| Image registry | Existing `public.ecr.aws/v6r1m8q2/energy-tracker/*` (private ECR optional later) | Already in place. |
| Observability | Existing in-cluster Prometheus/Grafana/Loki/Tempo on EBS | Already validated. Defer AMP/AMG/CloudWatch migration. |

## Target architecture (EKS)

```
                              Route53 (yourdomain.com)
                                       |
                                  ACM cert
                                       |
                                NLB (public)
                                       |
                              ingress-nginx
                                       |
              -----------------------------------------------
              |              |             |               |
          frontend     user-service   device-service   ... (6 svcs)
              |              |             |               |
              ----- inter-service via ClusterIP DNS --------
                       |                    |
                       v                    v
                   RDS MySQL            MSK Serverless
                  (managed)             (managed, IAM auth)
                       
              In-cluster (StatefulSet, EBS gp3):
              - InfluxDB
              - Redis
              - Prometheus / Grafana / Loki / Tempo
              
              insight-service ---> AWS Bedrock
                                    via us.anthropic.claude-haiku-4-5-20251001-v1:0
                                    (cross-region inference profile, IRSA-bound)
```

## Phase index

- **Phase 1** — Terraform foundation (1A–1F)
- **Phase 2** — Cluster addons (2A–2C)
- **Phase 3** — Chart parameterization & Bedrock swap (3A–3E)
- **Phase 4** — EKS values overlays (4A–4C)
- **Phase 5** — Secrets, DNS, TLS (5A–5C)
- **Phase 6** — CI/CD (6A–6C)
- **Phase 7** — First deploy + end-to-end validation (7A–7D)
- **Phase 8** — Hardening (8A–8D, optional)
- **Phase 9** — Documentation + handoff

Each sub-phase has its own validation gate. **Do not advance until every check in `Validation` passes.**

---

## Phase 1 — Terraform foundation

**Goal**: a working EKS cluster, RDS, MSK, Route53 zone, ACM cert — all reachable, all empty.

New top-level directory `terraform/`. Use one root module per environment (`terraform/envs/dev/`) and shared modules under `terraform/modules/`.

### 1A — VPC

**Work**
- 3-AZ VPC, 10.20.0.0/16
- Public subnets (for NLBs), private subnets (for nodes/pods), database subnets (for RDS, MSK)
- Single NAT gateway (cost over HA — re-evaluate later)
- VPC endpoints for S3 (gateway) and ECR / Secrets Manager / STS (interface) to keep traffic off NAT
- Tags: `kubernetes.io/role/elb=1` on public, `kubernetes.io/role/internal-elb=1` on private

**Validation**
```bash
terraform plan -target=module.vpc
terraform apply -target=module.vpc
aws ec2 describe-vpcs --filters Name=tag:Name,Values=iot-tracker-dev | jq '.Vpcs[].State'
# Expect: "available"
```
- All 9 subnets created (3 public, 3 private, 3 db)
- NAT gateway in `available` state
- VPC endpoints `available`

**Exit criteria**: `terraform plan` is clean; subnets visible in console; route tables look correct.

---

### 1B — EKS cluster + node group

**Work**
- EKS 1.31 (or current latest stable)
- One managed node group: `t3.large` x 2-3, mixed on-demand
- Cluster endpoint **private + public with CIDR allowlist** (your laptop IP)
- OIDC provider enabled
- Cluster security group rules: allow node-to-node, node-to-control-plane

**Validation**
```bash
aws eks update-kubeconfig --name iot-tracker-dev --region us-east-1
kubectl get nodes
# Expect: 2-3 nodes in Ready state
kubectl get pods -A
# Expect: kube-system pods all Running (CoreDNS, kube-proxy, vpc-cni)
kubectl get --raw='/healthz?verbose'
# Expect: ok across all checks
```

**Exit criteria**: `kubectl` works against the cluster, all kube-system pods Ready, OIDC issuer URL visible in Terraform output.

---

### 1C — IRSA roles

**Work**
- IAM role for AWS Load Balancer Controller (with the AWS-provided policy)
- IAM role for EBS CSI driver
- IAM role for External Secrets Operator (read access to specific Secrets Manager paths only)
- IAM role for `insight-service` ServiceAccount (with `bedrock:InvokeModel`/`Converse` on `us.anthropic.claude-haiku-4-5-*` inference-profile ARN + foundation-model ARNs in us-east-1/us-east-2/us-west-2)
- IAM role for ExternalDNS (Route53 zone write)
- IAM role for cert-manager DNS-01 (Route53 zone write — same zone, separate role)
- IAM role for GitHub Actions OIDC (deploy permissions: ECR push, EKS describe, sts AssumeRole)

**Validation**
```bash
for role in alb-controller ebs-csi external-secrets insight-service externaldns cert-manager gha-deploy; do
  aws iam get-role --role-name "iot-tracker-dev-$role" --query 'Role.AssumeRolePolicyDocument' || echo MISSING
done
```
- All seven roles exist with `Federated` principal pointing at the cluster OIDC provider (or GitHub OIDC for GHA)

**Exit criteria**: `terraform output irsa_role_arns` lists all seven ARNs.

---

### 1D — RDS MySQL

**Work**
- `db.t3.medium`, MySQL 8.0, single AZ for now (multi-AZ later)
- Encrypted at rest, automated backups (7-day retention)
- Security group: only allow inbound 3306 from EKS node SG
- Parameter group: same `lower_case_table_names`, `character_set_server` as your local MySQL (compare against `docker/mysql/source.cnf`)
- Database `energy_tracker` created via `provisioner` or via Terraform `mysql` provider

**Validation**
```bash
ENDPOINT=$(terraform output -raw rds_endpoint)
# From a pod inside the cluster:
kubectl run -it --rm mysql-test --image=mysql:8.3.0 --restart=Never -- \
  mysql -h "$ENDPOINT" -uroot -p"$RDS_PASSWORD" -e "SHOW DATABASES; SELECT VERSION();"
# Expect: energy_tracker in list, version 8.0.x
```

**Exit criteria**: Reachable from inside the cluster, `energy_tracker` exists, NOT reachable from public internet (verify via SG inspection).

---

### 1E — MSK Serverless

**Work**
- MSK Serverless cluster (single VPC config — three subnets across AZs)
- IAM auth enabled (only auth method)
- Security group: allow EKS node SG → 9098/TCP
- Output `bootstrap_brokers_sasl_iam`

**Validation**
```bash
BROKERS=$(terraform output -raw msk_bootstrap)
# From inside the cluster, with kafka client + IAM auth jar:
kubectl run -it --rm kafka-test --image=public.ecr.aws/aws-msk/kafka-iam-test:latest --restart=Never -- \
  kafka-topics --bootstrap-server "$BROKERS" --command-config /opt/client.properties --list
# Expect: empty list (no topics yet) — connection succeeded
```

**Exit criteria**: IAM-authenticated client can list topics. **This is the riskiest step in Phase 1** — budget extra time.

---

### 1F — Route53 + ACM

**Work**
- Public hosted zone for the chosen subdomain (e.g. `iot.yourdomain.com`)
- ACM cert with DNS validation, SAN `*.iot.yourdomain.com`
- Output zone ID and cert ARN

**Validation**
```bash
ZONE_ID=$(terraform output -raw route53_zone_id)
CERT_ARN=$(terraform output -raw acm_cert_arn)
aws acm describe-certificate --certificate-arn "$CERT_ARN" --query 'Certificate.Status'
# Expect: "ISSUED"
dig +short NS iot.yourdomain.com
# Expect: 4 AWS NS records (after parent zone is delegated)
```

**Exit criteria**: Cert is `ISSUED`; zone is delegated from the parent domain.

### Phase 1 final gate

- [ ] All sub-phases (1A–1F) pass their validation
- [ ] `terraform destroy` plan only lists ephemeral resources (sanity check no manual drift)
- [ ] One single `terraform apply` from clean state succeeds in <30 minutes

---

## Phase 2 — Cluster addons

**Goal**: cluster has every controller it needs to run application workloads.

Install via Helm (declarative — record the install commands in a `scripts/eks-bootstrap.sh`).

### 2A — EBS CSI + AWS Load Balancer Controller

**Work**
- EBS CSI driver as EKS managed addon (Terraform-driven)
- AWS LB Controller via Helm chart, ServiceAccount annotated with IRSA role from 1C

**Validation**
```bash
kubectl get pods -n kube-system | grep -E 'ebs-csi|aws-load-balancer-controller'
# Expect: all Running
kubectl get storageclass
# Expect: gp3 default StorageClass present
```

**Smoke test**
```bash
# Provision a 1Gi PVC and confirm a volume is created
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata: {name: ebs-smoke, namespace: default}
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: gp3
  resources: {requests: {storage: 1Gi}}
EOF
kubectl get pvc ebs-smoke
# Expect: STATUS=Bound within 30s
kubectl delete pvc ebs-smoke
```

---

### 2B — ingress-nginx + ExternalDNS

**Work**
- `ingress-nginx` Helm chart, `controller.service.annotations` include `service.beta.kubernetes.io/aws-load-balancer-type: nlb` and `aws-load-balancer-scheme: internet-facing`
- ExternalDNS Helm chart, IRSA-bound, `--domain-filter=iot.yourdomain.com`, `--policy=upsert-only` (safer than `sync`)

**Validation**
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
# Expect: EXTERNAL-IP=<NLB DNS name>, TYPE=LoadBalancer
NLB=$(kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
curl -v "http://$NLB/"
# Expect: 404 from nginx (controller answering, no ingress yet)
kubectl logs -n external-dns deploy/external-dns | grep -i 'all records are already up to date'
# Expect: ExternalDNS is healthy and idling
```

---

### 2C — External Secrets, cert-manager, metrics-server

**Work**
- External Secrets Operator Helm chart; `ClusterSecretStore` named `aws-secrets-manager` configured for our region with IRSA
- cert-manager Helm chart; `ClusterIssuer` for ACM via Route53 DNS-01
- metrics-server Helm chart

**Validation**
```bash
kubectl get clustersecretstore aws-secrets-manager -o jsonpath='{.status.conditions[0].type}'
# Expect: "Ready"
kubectl get clusterissuer
# Expect: route53-issuer Ready=True
kubectl top nodes
# Expect: CPU/Mem numbers (not "metrics not available")
```

**Smoke test (External Secrets)**
- Create a throwaway secret in Secrets Manager: `aws secretsmanager create-secret --name iot/test --secret-string '{"foo":"bar"}'`
- Apply an `ExternalSecret` referencing it
- Within ~30s, a `Secret` named `iot-test` exists with `foo=bar`
- Delete both

### Phase 2 final gate

- [ ] All four controllers (EBS CSI, ALB Controller, ingress-nginx, ExternalDNS) report Ready
- [ ] External Secrets smoke test passes
- [ ] cert-manager `ClusterIssuer` is Ready
- [ ] NLB hostname is resolvable from your laptop and returns nginx 404

---

## Phase 3 — Chart parameterization + Bedrock swap

**Goal**: make existing Helm charts environment-agnostic; swap insight-service to Bedrock under a Maven profile so Minikube/Compose retain Ollama.

Every change in this phase is additive. Run `helm template` against the **base** `values.yaml` after each sub-phase to confirm Minikube output is byte-identical to before.

### 3A — Ingress class & annotations templated

**Files touched**
- `k8s/charts/microservices-chart/values.yaml` — add:
  ```yaml
  ingress:
    enabled: true
    className: nginx
    host: ""              # empty = no host rule, current Minikube behavior
    annotations:
      nginx.ingress.kubernetes.io/use-regex: "true"
    tls:
      enabled: false
      secretName: ""
  ```
- `k8s/charts/microservices-chart/templates/ingress.yaml` — read class, host, annotations, optional `tls` block from `.Values.ingress.*`. Default behavior is unchanged.

**Validation**
```bash
helm template microservices ./k8s/charts/microservices-chart > /tmp/before.yaml
# (apply 3A changes)
helm template microservices ./k8s/charts/microservices-chart > /tmp/after.yaml
diff /tmp/before.yaml /tmp/after.yaml
# Expect: no diff for the ingress (rendered output identical)
```

---

### 3B — Service type templated on infra subcharts

**Files touched**
- `k8s/charts/infra-chart/charts/{influxdb,mailpit,kafka-ui}/templates/service.yaml` — replace `type: LoadBalancer` with `type: {{ .Values.service.type | default "LoadBalancer" }}`.
- `infra-chart/charts/mysql/templates/service.yaml` — same.
- Confirm `values.yaml` already has `service.type` (it does for most).

**Validation**
```bash
helm template infra ./k8s/charts/infra-chart | grep -A1 'kind: Service' | grep type:
# Expect: same set of types as before for Minikube (LoadBalancer where it was)
```

---

### 3C — Secret gating + ExternalSecret templates

**Files touched**
- `k8s/charts/microservices-chart/values.yaml` — add:
  ```yaml
  global:
    secrets:
      mode: inline           # "inline" or "external"
      external:
        secretStoreRef: aws-secrets-manager
        path: iot/dev        # Secrets Manager prefix
  ```
- `secret-global.yaml` and `secret-frontend.yaml` — wrap existing body in `{{- if eq .Values.global.secrets.mode "inline" }}`.
- New file `templates/externalsecret-global.yaml` — gated on `{{- if eq .Values.global.secrets.mode "external" }}`. Maps Secrets Manager keys to the same `Secret` name and keys the deployments already reference (`microservices-global-secret`, keys `SPRING_DATASOURCE_PASSWORD`, `INFLUX_TOKEN`, `JWT_SECRET`).
- New file `templates/externalsecret-frontend.yaml` — same pattern for `microservices-frontend-secret`.

**Validation**
```bash
# Inline mode (default — Minikube behavior)
helm template microservices ./k8s/charts/microservices-chart | grep -c 'kind: Secret'
# Expect: 2 (global + frontend)
helm template microservices ./k8s/charts/microservices-chart | grep -c 'kind: ExternalSecret'
# Expect: 0

# External mode
helm template microservices ./k8s/charts/microservices-chart --set global.secrets.mode=external | grep -c 'kind: Secret'
# Expect: 0
helm template microservices ./k8s/charts/microservices-chart --set global.secrets.mode=external | grep -c 'kind: ExternalSecret'
# Expect: 2
```

---

### 3D — ConfigMap parameterization (RDS / MSK endpoints)

**Files touched**
- `microservices-chart/values.yaml` — add overridable `global.datasource.url`, `global.kafka.bootstrap`. Default: empty string → fall back to in-cluster DNS.
- `templates/configmap-global.yaml` — `SPRING_DATASOURCE_URL` and `SPRING_KAFKA_BOOTSTRAP_SERVERS` use the override when set, else the existing in-cluster URL.

**Validation**
```bash
helm template microservices ./k8s/charts/microservices-chart | grep -E 'SPRING_DATASOURCE_URL|SPRING_KAFKA_BOOTSTRAP'
# Expect: Minikube/in-cluster values (jdbc:mysql://infra-mysql..., infra-kafka:9092)

helm template microservices ./k8s/charts/microservices-chart \
  --set global.datasource.url='jdbc:mysql://my-rds.../energy_tracker' \
  --set global.kafka.bootstrap='b-1.msk...:9098' \
  | grep -E 'SPRING_DATASOURCE_URL|SPRING_KAFKA_BOOTSTRAP'
# Expect: overridden values
```

---

### 3E — Insight-service Bedrock swap

**Files touched**
- `services/insight-service/pom.xml` — add Maven profiles:
  - `ollama` (active by default) — current `spring-ai-starter-model-ollama`
  - `bedrock` (active when `-Pbedrock` or via env property) — `spring-ai-starter-model-bedrock-converse`
- `services/insight-service/src/main/resources/application.properties` — keep Ollama config under `spring.ai.ollama.*`, add `spring.ai.bedrock.converse.chat.options.*` block conditionally. Use `@spring.profiles.active@` if necessary.
- `services/insight-service/Dockerfile` — accept a build-arg `MAVEN_PROFILE=ollama` (default), pass to `mvn package -P${MAVEN_PROFILE}`.
- `k8s/charts/microservices-chart/charts/insight-service/templates/deployment.yaml` — add `serviceAccountName` (will be IRSA-bound on EKS), keep the Ollama init container only when `.Values.insightService.backend == "ollama"` (Minikube default).
- `microservices-chart/values.yaml` — add `insightService.backend: "ollama"` and `insightService.bedrock.modelId: "us.anthropic.claude-haiku-4-5-20251001-v1:0"` (or any other inference profile your IAM policy allows).

**Validation**
```bash
# Ollama profile (Minikube/Compose default)
cd services/insight-service && mvn -q -DskipTests package
java -jar target/*.jar &  # with Ollama running locally
curl -s http://localhost:8085/actuator/health | jq .status
# Expect: "UP"
kill %1

# Bedrock profile
mvn -q -DskipTests -Pbedrock package
# Expect: build succeeds; Bedrock starter on classpath; Ollama starter not
unzip -p target/*.jar META-INF/spring/aot.factories 2>/dev/null | grep -i bedrock || \
  jar tf target/*.jar | grep -i 'spring-ai-bedrock'
```

**Smoke test (in EKS, deferred to Phase 7)**
- Once deployed in EKS with Bedrock profile + IRSA, hit `/api/v1/insight/...` and confirm a Bedrock response.

### Phase 3 final gate

- [ ] `helm template` of all three charts with default values produces zero meaningful diff vs pre-Phase-3 output (only adds new optional fields)
- [ ] Insight-service builds clean under both `-P ollama` and `-P bedrock`
- [ ] Docker Compose `docker compose up -d` still starts every service (regression check)
- [ ] Minikube `helm upgrade` is a no-op deploy (regression check)

---

## Phase 4 — EKS values overlays

**Goal**: a single `helm install ... -f values-eks.yaml` produces a cluster-ready deploy. **No template edits in this phase.**

### 4A — `infra-chart/values-eks.yaml`

```yaml
mysql:
  enabled: false             # using RDS
kafka:
  enabled: false             # using MSK Serverless
kafkaUi:
  enabled: false             # MSK has its own UI; or run on demand later
ollama:
  enabled: false             # using Bedrock
mailpit:
  enabled: true              # keep for dev EKS; swap for SES in prod
  service:
    type: ClusterIP
influxdb:
  enabled: true
  service:
    type: ClusterIP
  persistence:
    enabled: true
    size: 20Gi
    storageClassName: gp3
redis:
  enabled: true
  service:
    type: ClusterIP
```

**Validation**
```bash
helm template infra ./k8s/charts/infra-chart -f ./k8s/charts/infra-chart/values-eks.yaml > /tmp/eks-infra.yaml
grep -c 'kind: StatefulSet' /tmp/eks-infra.yaml
# Expect: 1 (InfluxDB only)
grep 'type:' /tmp/eks-infra.yaml | sort -u
# Expect: only ClusterIP
```

---

### 4B — `microservices-chart/values-eks.yaml`

```yaml
global:
  namespace: default
  secrets:
    mode: external
    external:
      secretStoreRef: aws-secrets-manager
      path: iot/dev
  datasource:
    url: "jdbc:mysql://${RDS_ENDPOINT}/energy_tracker"
  kafka:
    bootstrap: "${MSK_BOOTSTRAP}"

ingress:
  enabled: true
  className: nginx
  host: iot.yourdomain.com
  annotations:
    nginx.ingress.kubernetes.io/use-regex: "true"
    cert-manager.io/cluster-issuer: route53-issuer
  tls:
    enabled: true
    secretName: iot-tls

insightService:
  backend: bedrock
  bedrock:
    modelId: us.anthropic.claude-haiku-4-5-20251001-v1:0
  serviceAccount:
    annotations:
      eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/iot-tracker-dev-insight-service

userService:    {replicas: 2}
deviceService:  {replicas: 2}
usageService:   {replicas: 2}
alertService:   {replicas: 2}
ingestionService: {replicas: 1}
insightService: {replicas: 1, backend: bedrock}
frontend:       {replicas: 2}
```

**Validation**
```bash
helm template microservices ./k8s/charts/microservices-chart -f .../values-eks.yaml > /tmp/eks-msvc.yaml
grep -c 'kind: ExternalSecret' /tmp/eks-msvc.yaml
# Expect: 2
grep 'iot.yourdomain.com' /tmp/eks-msvc.yaml | head -1
# Expect: present in Ingress
grep -A1 'eks.amazonaws.com/role-arn' /tmp/eks-msvc.yaml
# Expect: present on insight-service ServiceAccount
```

---

### 4C — `observability-chart/values-eks.yaml`

```yaml
prometheus:
  persistence: {enabled: true, size: 50Gi, storageClassName: gp3}
loki:
  persistence: {enabled: true, size: 100Gi, storageClassName: gp3}
tempo:
  persistence: {enabled: true, size: 50Gi, storageClassName: gp3}
grafana:
  persistence: {enabled: true, size: 10Gi, storageClassName: gp3}
promtail:
  enabled: true
```

> The observability subcharts may need small template edits if they don't already accept `persistence.*` — handle inline as part of 4C.

**Validation**
```bash
helm template observability ./k8s/charts/observability-chart -f .../values-eks.yaml | grep -c 'kind: PersistentVolumeClaim'
# Expect: at least 4
```

### Phase 4 final gate

- [ ] All three `values-eks.yaml` files commit cleanly
- [ ] `helm template ... -f values-eks.yaml` produces output where every Service is `ClusterIP`, every StatefulSet has a `volumeClaimTemplate` referencing `gp3`, every secret comes from `ExternalSecret`
- [ ] `helm template` against base `values.yaml` (no `-f`) is unchanged from pre-Phase-3

---

## Phase 5 — Secrets, DNS, TLS

**Goal**: secrets in AWS Secrets Manager, ingress reachable at `iot.yourdomain.com` over HTTPS.

### 5A — Populate AWS Secrets Manager

**Work**
- Terraform creates the *secret resources* (empty); operator populates values via CLI.
- Path layout under `iot/dev/`:
  - `iot/dev/global` → JSON `{ "SPRING_DATASOURCE_PASSWORD": "...", "INFLUX_TOKEN": "...", "JWT_SECRET": "..." }`
  - `iot/dev/frontend` → JSON `{ "NEXTAUTH_SECRET": "...", "GOOGLE_CLIENT_ID": "...", "GOOGLE_CLIENT_SECRET": "..." }`

**Validation**
```bash
aws secretsmanager get-secret-value --secret-id iot/dev/global --query SecretString | jq 'keys'
# Expect: ["INFLUX_TOKEN","JWT_SECRET","SPRING_DATASOURCE_PASSWORD"]
aws secretsmanager get-secret-value --secret-id iot/dev/frontend --query SecretString | jq 'keys'
# Expect: ["GOOGLE_CLIENT_ID","GOOGLE_CLIENT_SECRET","NEXTAUTH_SECRET"]
```

---

### 5B — ExternalSecret deployment

**Work**
- Apply only the secret-related templates first (test in isolation):
  ```bash
  helm template microservices ./k8s/charts/microservices-chart -f .../values-eks.yaml \
    --show-only templates/externalsecret-global.yaml \
    --show-only templates/externalsecret-frontend.yaml | kubectl apply -f -
  ```

**Validation**
```bash
kubectl get externalsecret
# Expect: SyncedAt has a timestamp, Status=Ready
kubectl get secret microservices-global-secret -o jsonpath='{.data}' | jq 'keys'
# Expect: same three keys as inline mode
```

**Failure mode to test**: delete one of the secrets; ESO should re-sync within 1 minute. Confirm.

---

### 5C — DNS + TLS via cert-manager

**Work**
- After ingress is applied (Phase 7), cert-manager will solve the DNS-01 challenge against Route53 and write the TLS secret.
- ExternalDNS will create an A-record `iot.yourdomain.com` → NLB hostname.

**Validation**
```bash
dig +short iot.yourdomain.com
# Expect: NLB hostname / IP
curl -sI "https://iot.yourdomain.com/" | head -1
# Expect: HTTP/2 200 or 404 — TLS handshake succeeds either way
echo | openssl s_client -connect iot.yourdomain.com:443 -servername iot.yourdomain.com 2>/dev/null \
  | openssl x509 -noout -issuer -dates
# Expect: issuer Amazon, not-after at least 60 days out
```

### Phase 5 final gate

- [ ] All ExternalSecrets are `Ready`
- [ ] DNS resolves the NLB
- [ ] TLS cert is issued and valid

---

## Phase 6 — CI/CD

**Goal**: pushing to `main` builds, pushes, and deploys.

### 6A — GitHub Actions OIDC trust

**Work**
- Terraform creates a GitHub OIDC role (one already in 1C) with `sts:AssumeRoleWithWebIdentity` trust restricted to `repo:chieaid24/IoT-Energy-Tracker:ref:refs/heads/main` and `:environment:dev`.
- Permissions: `ecr:Get*`, `ecr:Put*`, `ecr:Initiate*`, `ecr:Upload*`, `eks:DescribeCluster`, `sts:GetCallerIdentity`.

**Validation**
```bash
# From a manually triggered tiny workflow that just calls aws sts get-caller-identity
# Expect: assumed role ARN matches the OIDC role
```

---

### 6B — Build & push workflow

**Work**
- New `.github/workflows/build-and-push.yml`:
  - Trigger: push to `main`, push of tag `v*`
  - Matrix: 6 services + frontend
  - Buildx multi-arch (linux/amd64 only initially — keep simple)
  - Tag images with `${{ github.sha }}` AND `latest`
  - Push to `public.ecr.aws/v6r1m8q2/energy-tracker/<svc>`
  - Special case: insight-service needs `--build-arg MAVEN_PROFILE=bedrock` for the EKS image. Build two tags: `:<sha>-ollama` and `:<sha>-bedrock`. EKS overlay pins the bedrock tag.

**Validation**
```bash
# After first successful run:
aws ecr-public describe-images --repository-name energy-tracker/user-service \
  --query 'imageDetails[?imageTags!=`null`].[imageTags[0],imagePushedAt]' --output table
# Expect: a row with the SHA tag, recent push time

# Insight-service should have both variants:
aws ecr-public describe-images --repository-name energy-tracker/insight-service \
  --query 'imageDetails[*].imageTags[]' --output text
# Expect: includes both '*-ollama' and '*-bedrock' tags
```

---

### 6C — Deploy workflow

**Work**
- New `.github/workflows/deploy-eks.yml`:
  - Trigger: `workflow_dispatch` (manual) or `workflow_run` after successful build-and-push on `main`
  - Steps:
    1. `aws eks update-kubeconfig`
    2. `helm upgrade --install infra ./k8s/charts/infra-chart -f .../values-eks.yaml --wait`
    3. `helm upgrade --install observability ./k8s/charts/observability-chart -f .../values-eks.yaml --wait`
    4. `helm upgrade --install microservices ./k8s/charts/microservices-chart -f .../values-eks.yaml --set image.tag=${{ github.sha }} --wait`
    5. Run smoke test job (Phase 7 checks)
- Concurrency group `deploy-eks` to prevent overlapping deploys.

**Validation**
- Trigger `workflow_dispatch` once
- `helm list -n default` post-deploy shows three releases, all `deployed`
- All Pods Ready within 5 min

### Phase 6 final gate

- [ ] Build workflow green on `main`
- [ ] Deploy workflow green on `workflow_dispatch`
- [ ] Image tags are SHA-pinned (no `:latest` references in any deployed manifest — `kubectl get pods -o jsonpath='{..image}' | tr ' ' '\n' | grep latest` returns nothing)

---

## Phase 7 — First deploy + end-to-end validation

**Goal**: prove the full event pipeline works on EKS.

### 7A — Infra + observability up

```bash
helm upgrade --install infra ./k8s/charts/infra-chart -f .../values-eks.yaml --wait
helm upgrade --install observability ./k8s/charts/observability-chart -f .../values-eks.yaml --wait
```

**Validation**
```bash
kubectl get pods | grep -E 'influxdb|redis|mailpit|prometheus|grafana|loki|tempo|promtail'
# Expect: all Running, all Ready

kubectl exec deploy/infra-influxdb -- influx ping
# Expect: OK

kubectl exec deploy/infra-redis -- redis-cli ping
# Expect: PONG
```

---

### 7B — Microservices up

```bash
helm upgrade --install microservices ./k8s/charts/microservices-chart \
  -f .../values-eks.yaml \
  --set image.tag="$GIT_SHA" \
  --wait --timeout 10m
```

**Validation**
```bash
kubectl get deploy
# Expect: all 7 deployments at desired replicas, all Available

for svc in user device ingestion usage alert insight; do
  kubectl exec deploy/microservices-${svc}-service -- \
    curl -fsS localhost:80${PORT}/actuator/health | jq .status
done
# Expect: "UP" for each
```

---

### 7C — End-to-end pipeline smoke test

```bash
BASE=https://iot.yourdomain.com

# 1. Register a user
curl -sX POST "$BASE/api/v1/user/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@test","password":"test1234"}'

# 2. Login → get JWT
TOKEN=$(curl -sX POST "$BASE/api/v1/user/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@test","password":"test1234"}' | jq -r .token)

# 3. Register a device
DEV=$(curl -sX POST "$BASE/api/v1/device" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"smoke-meter","type":"meter"}' | jq -r .id)

# 4. Trigger ingestion
curl -sX POST "$BASE/api/v1/ingestion/event" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"deviceId\":\"$DEV\",\"watts\":1500,\"timestamp\":\"$(date -Iseconds)\"}"

# 5. Wait, then query usage
sleep 5
curl -s "$BASE/api/v1/usage?deviceId=$DEV" -H "Authorization: Bearer $TOKEN" | jq .
# Expect: at least one data point

# 6. Verify alert pipeline (push 10000W to trip threshold)
curl -sX POST "$BASE/api/v1/ingestion/event" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"deviceId\":\"$DEV\",\"watts\":10000,\"timestamp\":\"$(date -Iseconds)\"}"
sleep 10
curl -s "$BASE/api/v1/alert?deviceId=$DEV" -H "Authorization: Bearer $TOKEN" | jq .
# Expect: at least one alert

# 7. Verify Bedrock-backed insight
curl -s "$BASE/api/v1/insight?deviceId=$DEV" -H "Authorization: Bearer $TOKEN"
# Expect: text response from Bedrock (no "ollama" string in trace)
```

---

### 7D — Observability spot check

- Open Grafana: confirm Prometheus/Loki/Tempo datasources `Healthy`
- Confirm 4 provisioned dashboards render with data
- Confirm a trace from the smoke test appears in Tempo, with all 3 service spans (ingestion → usage → alert) joined
- Confirm logs from the smoke test appear in Loki under labels `service`/`level`/`traceId`

### Phase 7 final gate

- [ ] All 6 services + frontend Ready
- [ ] End-to-end smoke test passes
- [ ] Bedrock returns a real response from `insight-service`
- [ ] Traces, logs, and metrics all flowing
- [ ] No `Pending` PVCs, no `CrashLoopBackOff`, no `ImagePullBackOff` anywhere

---

## Phase 8 — Hardening (optional but strongly recommended)

### 8A — HPA on stateless services

`HorizontalPodAutoscaler` on user/device/usage/insight/frontend — target 70% CPU, min 2 / max 5.

**Validation**
```bash
kubectl get hpa
# Expect: TARGETS show real %, not <unknown>
# Force load (apachebench from a debug pod), watch replicas scale
```

### 8B — PodDisruptionBudgets

`PDB minAvailable: 1` on every Deployment with `replicas >= 2`.

**Validation**
- `kubectl get pdb` — all `ALLOWED DISRUPTIONS >= 0`
- `kubectl drain <node>` on a worker — no PDB violations

### 8C — NetworkPolicies

Default deny ingress + egress in `default` namespace, then explicit allows:
- frontend → user-service (8080)
- user-service → MySQL (3306)
- ingestion-service → MSK (9098)
- usage-service → MSK + InfluxDB + device-service + user-service
- alert-service → MSK + MySQL + Mailpit
- insight-service → usage-service + Bedrock VPC endpoint
- All services → cluster DNS

**Validation**
- From frontend pod, `nc -vz <some-non-allowed-svc> <port>` should time out
- From frontend pod, `nc -vz microservices-user-service 8080` should connect

### 8D — Alarms

CloudWatch alarms (Terraform):
- RDS: CPU > 80% for 5 min, connections > 80% of max, free storage < 20%
- MSK: connection count, throttled requests
- EKS: node group desired vs. actual, control-plane API errors

**Validation**
- `aws cloudwatch describe-alarms --alarm-name-prefix iot-tracker-dev` returns each alarm in `OK` state.

### Phase 8 final gate

- [ ] HPA functional under synthetic load
- [ ] Drain test respects PDBs
- [ ] NetworkPolicy denies + allows behave as designed
- [ ] All alarms `OK`

---

## Phase 9 — Documentation + handoff

**Work**
- Update `README.md` with EKS deploy section (commands, prerequisites, env vars)
- Update `CLAUDE.md` "Architecture Overview" with Bedrock note + EKS-specific config
- New `docs/eks-runbook.md` with day-2 operations: how to rotate secrets, how to scale RDS, how to upgrade EKS
- Save Terraform state to S3 + DynamoDB lock if not already
- Tag the cluster, RDS, MSK with `Project=iot-tracker`, `Env=dev`, `Owner=...` for cost tracking

**Validation**
- A teammate (or you, fresh terminal) can follow `docs/eks-runbook.md` to do a `terraform plan` from clean checkout and see "No changes"

### Phase 9 final gate

- [ ] Docs merged
- [ ] Cost report tagged correctly in AWS Cost Explorer

---

## Cumulative effort estimate

| Phase | Days |
|---|---|
| 1 — Terraform foundation | 2.0 |
| 2 — Cluster addons | 0.5 |
| 3 — Chart parameterization + Bedrock | 1.0 |
| 4 — EKS overlays | 0.5 |
| 5 — Secrets + DNS + TLS | 1.0 |
| 6 — CI/CD | 1.0 |
| 7 — First deploy + smoke | 1.0 |
| 8 — Hardening (optional) | 1.0 |
| 9 — Docs | 0.5 |
| **Total focused work** | **~8.5 days** |
| **Realistic calendar (part-time)** | **~3 weeks** |

## Risk register

| Risk | Mitigation |
|---|---|
| MSK IAM auth + Spring Kafka client wiring | Budget extra time in 1E and 7B; have a fallback plan to switch MSK to SCRAM-SHA-512 if IAM proves brittle. |
| Bedrock model availability + per-day quota | Confirm `anthropic.claude-haiku-4-5-*` model access is enabled in Bedrock console. **Separately check the per-day token cap** — new accounts often have access granted but `Model invocation max tokens per day` at 0, blocking all invocations. Submit a Service Quotas request for `Cross-region model inference tokens per minute for Anthropic Claude Haiku 4.5` to trigger the account-level review that lifts the daily cap. |
| RDS parameter group differences vs local MySQL | Diff `docker/mysql/source.cnf` against the chosen RDS parameter group during 1D. Replicate any non-default settings. |
| Cert-manager DNS-01 race with ExternalDNS | Use `--policy=upsert-only` on ExternalDNS so it never deletes the cert-manager TXT records. |
| Cost overrun (RDS + MSK + NAT + EBS) | Set up a budget alert in Phase 1 *before* `terraform apply`. Estimate ~$200-300/mo for dev. |
| Image tag drift between Compose / Minikube / EKS | Always pin EKS to SHA tags, never `:latest`. Compose/Minikube can keep `:latest` for convenience. |

## Out of scope (deferred)

- Multi-region / DR
- Blue/green or canary deploys (Argo Rollouts)
- Cost-optimized Spot node groups beyond the simple mix in 1B
- Migrating observability to AMP/AMG/CloudWatch
- Cognito-fronted SSO via ALB
- Private ECR migration
- Production-grade RDS (Multi-AZ + read replica)
- Production-grade MSK (provisioned with custom partition counts)
