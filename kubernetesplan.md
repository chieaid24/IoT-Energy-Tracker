# Kubernetes Migration Plan for Energy Tracker

## Overview

Migrate the Docker Compose-based energy tracker microservices application to Kubernetes running on **minikube** with:
- ✅ Full infrastructure migration (MySQL, Kafka, InfluxDB, Mailpit, Ollama)
- ✅ Helm charts for templating and package management
- ✅ nginx-ingress for routing
- ✅ Persistent storage for stateful services

## Current Architecture

**Infrastructure:** MySQL (3306), Kafka (9092/9094), InfluxDB (8086), Mailpit (8025/1025), Ollama (11434)
**Microservices:** user-service (8080), device-service (8081), ingestion-service (8082), usage-service (8083), alert-service (8084), insight-service (8085)

**Critical Bug to Fix:** `docker/mysql/init.sql` uses `home_energy_tracker` but should use `energy_tracker`

---

## PHASE 1: Container Registry & CI/CD

This phase sets up the container registry (Amazon ECR) and automated build pipeline (GitHub Actions) for all microservices. This is the foundation for containerizing the application before deploying to Kubernetes.

### Install AWS CLI

```bash
# Install AWS CLI (for ECR access)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify installation
aws --version
```

### Configure AWS Credentials

```bash
# Configure AWS credentials
aws configure
# Enter:
#   AWS Access Key ID: [Your IAM user access key]
#   AWS Secret Access Key: [Your IAM user secret key]
#   Default region name: us-east-1
#   Default output format: json

# Verify credentials
aws sts get-caller-identity
```

### Setup Amazon ECR Repositories

```bash
# Create ECR repositories for each microservice
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  aws ecr create-repository \
    --repository-name energy-tracker/$service \
    --region us-east-1 \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
done

# Get registry URL (save this for later)
export ECR_REGISTRY=$(aws ecr describe-repositories --region us-east-1 --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)
echo "ECR Registry: $ECR_REGISTRY"
echo "Save this URL - you'll need it for later phases"
```

### Create GitHub Actions Pipeline

Create `.github/workflows/build-and-push.yml`:

```yaml
name: Build and Push to ECR

on:
  push:
    branches:
      - main
      - develop
    paths:
      - '*-service/**'
      - '.github/workflows/build-and-push.yml'
  pull_request:
    branches:
      - main

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: <account-id>.dkr.ecr.us-east-1.amazonaws.com

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      user-service: ${{ steps.filter.outputs.user-service }}
      device-service: ${{ steps.filter.outputs.device-service }}
      ingestion-service: ${{ steps.filter.outputs.ingestion-service }}
      usage-service: ${{ steps.filter.outputs.usage-service }}
      alert-service: ${{ steps.filter.outputs.alert-service }}
      insight-service: ${{ steps.filter.outputs.insight-service }}
    steps:
      - uses: actions/checkout@v3
      - uses: dorny/paths-filter@v2
        id: filter
        with:
          filters: |
            user-service:
              - 'user-service/**'
            device-service:
              - 'device-service/**'
            ingestion-service:
              - 'ingestion-service/**'
            usage-service:
              - 'usage-service/**'
            alert-service:
              - 'alert-service/**'
            insight-service:
              - 'insight-service/**'

  build-and-push:
    needs: detect-changes
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - user-service
          - device-service
          - ingestion-service
          - usage-service
          - alert-service
          - insight-service
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Check if service changed
        id: changed
        run: |
          if [ "${{ needs.detect-changes.outputs[matrix.service] }}" == "true" ]; then
            echo "changed=true" >> $GITHUB_OUTPUT
          else
            echo "changed=false" >> $GITHUB_OUTPUT
          fi

      - name: Configure AWS credentials
        if: steps.changed.outputs.changed == 'true'
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        if: steps.changed.outputs.changed == 'true'
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1

      - name: Extract metadata (tags, labels)
        if: steps.changed.outputs.changed == 'true'
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.ECR_REGISTRY }}/energy-tracker/${{ matrix.service }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        if: steps.changed.outputs.changed == 'true'
        uses: docker/build-push-action@v4
        with:
          context: ./${{ matrix.service }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Image digest
        if: steps.changed.outputs.changed == 'true'
        run: echo "Image pushed with digest: ${{ steps.docker_build.outputs.digest }}"

  scan-images:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    strategy:
      matrix:
        service:
          - user-service
          - device-service
          - ingestion-service
          - usage-service
          - alert-service
          - insight-service
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Get scan results
        run: |
          aws ecr describe-image-scan-findings \
            --repository-name energy-tracker/${{ matrix.service }} \
            --image-id imageTag=latest \
            --region ${{ env.AWS_REGION }} || echo "Scan not complete yet"
```

**IMPORTANT:** Replace `<account-id>` in the ECR_REGISTRY environment variable with your actual AWS account ID.

### Setup GitHub Secrets

Add these secrets to your GitHub repository:
1. Go to: GitHub Repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add the following secrets:

- **Name:** `AWS_ACCESS_KEY_ID`
  **Value:** [Your IAM user access key]

- **Name:** `AWS_SECRET_ACCESS_KEY`
  **Value:** [Your IAM user secret key]

**Required IAM Permissions:**

Your IAM user needs the following permissions policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:DescribeImages",
        "ecr:DescribeImageScanFindings"
      ],
      "Resource": "*"
    }
  ]
}
```

### Initial Manual Build & Push

For the first deployment, manually build and push all images to ECR:

```bash
# Ensure you're in the project root
cd /home/noobi/projects/energy-tracker

# Login to ECR
export ECR_REGISTRY=$(aws ecr describe-repositories --region us-east-1 --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_REGISTRY

# Build and push each service
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  echo "Building and pushing $service..."
  docker build -t $ECR_REGISTRY/energy-tracker/$service:latest ./$service
  docker push $ECR_REGISTRY/energy-tracker/$service:latest
  echo "✅ Completed $service"
done

echo "All images pushed to ECR!"
```

### Verification

Verify all images are in ECR:

```bash
# List all repositories
aws ecr describe-repositories --region us-east-1 --query 'repositories[*].repositoryName' --output table

# Check images for each service
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  echo "Images in energy-tracker/$service:"
  aws ecr describe-images --repository-name energy-tracker/$service --region us-east-1 --query 'imageDetails[*].[imageTags[0],imagePushedAt]' --output table
done
```

### Test GitHub Actions Pipeline

```bash
# Make a small change to trigger the pipeline
echo "# Test ECR pipeline" >> README.md
git add README.md
git commit -m "test: trigger ECR build pipeline"
git push origin main

# Monitor the pipeline:
# 1. Go to GitHub → Actions tab
# 2. Watch the "Build and Push to ECR" workflow
# 3. Verify all services build and push successfully
```

### Phase 1 Success Criteria

✅ AWS CLI installed and configured
✅ 6 ECR repositories created (one for each microservice)
✅ `.github/workflows/build-and-push.yml` file created
✅ GitHub Secrets configured (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
✅ All 6 service images built and pushed to ECR with `latest` tag
✅ Images visible in ECR console or via AWS CLI
✅ GitHub Actions pipeline triggers on push and successfully builds/pushes images

**Next Phase:** Once all images are in ECR and the CI/CD pipeline is working, proceed to Phase 2 to set up the local Kubernetes environment.

---

## PHASE 2: Create Helm Chart Structure

### Directory Structure

```bash
cd /home/noobi/projects/energy-tracker
mkdir -p k8s/helm-charts/{infrastructure-chart,microservices-chart}/templates
```

**Infrastructure Chart:**
```
infrastructure-chart/
├── Chart.yaml
├── values.yaml              # Default values (shared)
├── values-minikube.yaml     # Minikube-specific overrides
├── values-eks.yaml          # EKS-specific overrides (for future)
├── templates/
│   ├── _helpers.tpl
│   ├── mysql/{configmap,secret,pvc,deployment,service}.yaml
│   ├── kafka/{configmap,pvc,statefulset,service}.yaml
│   ├── kafka-ui/{deployment,service}.yaml
│   ├── influxdb/{secret,pvc,deployment,service}.yaml
│   ├── mailpit/{deployment,service}.yaml
│   ├── ollama/{pvc,deployment,service}.yaml
│   └── ingress.yaml
```

**Microservices Chart:**
```
microservices-chart/
├── Chart.yaml
├── values.yaml              # Default values (shared)
├── values-minikube.yaml     # Minikube-specific overrides
├── values-eks.yaml          # EKS-specific overrides (for future)
├── templates/
│   ├── _helpers.tpl
│   ├── configmap-global.yaml
│   ├── secret-global.yaml
│   ├── user-service/{deployment,service}.yaml
│   ├── device-service/{deployment,service}.yaml
│   ├── ingestion-service/{configmap,deployment,service}.yaml
│   ├── usage-service/{deployment,service}.yaml
│   ├── alert-service/{deployment,service}.yaml
│   ├── insight-service/{deployment,service}.yaml
│   └── ingress.yaml
```

### Values File Strategy

**values.yaml (defaults)** - Environment-agnostic base configuration:
- Resource requests/limits (reasonable defaults)
- Replica counts (1 for most services)
- Image repositories and tags
- Feature flags (all enabled by default)
- Common annotations/labels

**values-minikube.yaml** - Minikube-specific overrides:
- `storageClass: standard` (minikube's default)
- `imagePullPolicy: IfNotPresent` (use local images)
- `imageRegistry: ""` (no registry prefix)
- Reduced resource limits (fits in 12GB RAM)
- Ingress host: `energy-tracker.local`
- NodePort services for debugging (optional)

**values-eks.yaml** - EKS-specific overrides (blank for now, populate later):
- `storageClass: gp3` (AWS EBS)
- `imagePullPolicy: Always` (pull from ECR)
- `imageRegistry: <account-id>.dkr.ecr.<region>.amazonaws.com/`
- Production resource limits
- Ingress host: `energy-tracker.<domain>.com`
- LoadBalancer services
- AWS-specific annotations (EBS CSI, ALB ingress, etc.)

### Example Values File Structure

**infrastructure-chart/values.yaml:**
```yaml
global:
  namespace: energy-tracker
  # storageClass: defined per environment

mysql:
  enabled: true
  image:
    repository: mysql
    tag: "8.3.0"
  persistence:
    size: 10Gi
  credentials:
    rootPassword: password
    database: energy_tracker
  resources:
    requests:
      memory: "512Mi"
      cpu: "500m"
    limits:
      memory: "1Gi"
      cpu: "1000m"
# ... other services
```

**infrastructure-chart/values-minikube.yaml:**
```yaml
global:
  storageClass: standard
  imagePullPolicy: IfNotPresent

# Reduce resources for local development
mysql:
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"

kafka:
  resources:
    requests:
      memory: "512Mi"
      cpu: "500m"
    limits:
      memory: "1Gi"
      cpu: "1000m"
```

**infrastructure-chart/values-eks.yaml:**
```yaml
# Leave blank for now - will populate during EKS migration
# Example future content:
#
# global:
#   storageClass: gp3
#   imagePullPolicy: Always
#
# mysql:
#   persistence:
#     storageClass: gp3-encrypted
#     size: 100Gi
#   resources:
#     requests:
#       memory: "2Gi"
#       cpu: "1000m"
#     limits:
#       memory: "4Gi"
#       cpu: "2000m"
```

### Key Files to Create

1. **infrastructure-chart/Chart.yaml**
2. **infrastructure-chart/values.yaml** - Environment-agnostic defaults
3. **infrastructure-chart/values-minikube.yaml** - Minikube overrides
4. **infrastructure-chart/values-eks.yaml** - EKS overrides (blank, for future)
5. **microservices-chart/Chart.yaml**
6. **microservices-chart/values.yaml** - Environment-agnostic defaults
7. **microservices-chart/values-minikube.yaml** - Minikube overrides
8. **microservices-chart/values-eks.yaml** - EKS overrides (blank, for future)

---

## PHASE 3: Infrastructure Migration

### MySQL (Fix init.sql Bug!)

**Critical:** Create ConfigMap with FIXED init.sql:

```yaml
# templates/mysql/configmap.yaml
data:
  init.sql: |
    CREATE DATABASE IF NOT EXISTS energy_tracker;
    USE energy_tracker;  # FIXED: was home_energy_tracker
```

Create: Secret (root/password), PVC (10Gi), Deployment (with init script mount), Service (ClusterIP:3306)

**Health Check:**
```yaml
readinessProbe:
  exec:
    command: ["mysqladmin", "ping", "-h", "localhost", "-uroot", "-p$(MYSQL_ROOT_PASSWORD)"]
  initialDelaySeconds: 40
```

### Kafka (KRaft Mode)

Create: ConfigMap (KRaft config), PVC (10Gi), StatefulSet, Service (headless + ClusterIP:9092)

**Key Config:**
- `KAFKA_CLUSTER_ID: energy-tracker-cluster-1`
- `KAFKA_PROCESS_ROLES: broker,controller`
- `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka.energy-tracker.svc.cluster.local:9092`
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`

### InfluxDB

Create: Secret (admin/admin123, token: my-token), PVC (10Gi), Deployment, Service (ClusterIP:8086)

**Environment:**
```yaml
env:
  - name: DOCKER_INFLUXDB_INIT_MODE
    value: "setup"
  - name: DOCKER_INFLUXDB_INIT_ORG
    value: "chieaid24"
  - name: DOCKER_INFLUXDB_INIT_BUCKET
    value: "usage-bucket"
  - name: DOCKER_INFLUXDB_INIT_RETENTION
    value: "1w"
```

### Mailpit, Kafka-UI, Ollama

- **Mailpit:** Simple deployment, ports 8025 (web) + 1025 (SMTP)
- **Kafka-UI:** Point to `kafka.energy-tracker.svc.cluster.local:9092`
- **Ollama:** PVC (10Gi), initContainer to pull `deepseek-r1`, main container serves on 11434

### Infrastructure Ingress

```yaml
# templates/ingress.yaml
rules:
- host: energy-tracker.local
  paths:
  - path: /kafka-ui(/|$)(.*)
    backend: kafka-ui:8080
  - path: /mailpit(/|$)(.*)
    backend: mailpit:8025
  - path: /influxdb(/|$)(.*)
    backend: influxdb:8086
```

### Deploy Infrastructure

```bash
cd /home/noobi/projects/energy-tracker

# Deploy with minikube-specific values
helm install infrastructure ./k8s/helm-charts/infrastructure-chart \
  --create-namespace \
  --namespace energy-tracker \
  --values ./k8s/helm-charts/infrastructure-chart/values.yaml \
  --values ./k8s/helm-charts/infrastructure-chart/values-minikube.yaml \
  --wait

# Verify
kubectl get pods,svc,pvc -n energy-tracker
kubectl wait --for=condition=ready pod -l app=mysql -n energy-tracker --timeout=300s
```

**Note:** When migrating to EKS, use:
```bash
helm install infrastructure ./k8s/helm-charts/infrastructure-chart \
  --values ./k8s/helm-charts/infrastructure-chart/values.yaml \
  --values ./k8s/helm-charts/infrastructure-chart/values-eks.yaml \
  --namespace energy-tracker
```

---

## PHASE 4: Container Registry & CI/CD

### Setup Amazon ECR Repositories

```bash
# Login to AWS CLI (ensure you have credentials configured)
aws configure

# Create ECR repositories for each microservice
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  aws ecr create-repository \
    --repository-name energy-tracker/$service \
    --region us-east-1 \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
done

# Get registry URL
export ECR_REGISTRY=$(aws ecr describe-repositories --region us-east-1 --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)
echo "ECR Registry: $ECR_REGISTRY"
```

### Create GitHub Actions Pipeline

Create `.github/workflows/build-and-push.yml`:

```yaml
name: Build and Push to ECR

on:
  push:
    branches:
      - main
      - develop
    paths:
      - '*-service/**'
      - '.github/workflows/build-and-push.yml'
  pull_request:
    branches:
      - main

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: <account-id>.dkr.ecr.us-east-1.amazonaws.com

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      user-service: ${{ steps.filter.outputs.user-service }}
      device-service: ${{ steps.filter.outputs.device-service }}
      ingestion-service: ${{ steps.filter.outputs.ingestion-service }}
      usage-service: ${{ steps.filter.outputs.usage-service }}
      alert-service: ${{ steps.filter.outputs.alert-service }}
      insight-service: ${{ steps.filter.outputs.insight-service }}
    steps:
      - uses: actions/checkout@v3
      - uses: dorny/paths-filter@v2
        id: filter
        with:
          filters: |
            user-service:
              - 'user-service/**'
            device-service:
              - 'device-service/**'
            ingestion-service:
              - 'ingestion-service/**'
            usage-service:
              - 'usage-service/**'
            alert-service:
              - 'alert-service/**'
            insight-service:
              - 'insight-service/**'

  build-and-push:
    needs: detect-changes
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - user-service
          - device-service
          - ingestion-service
          - usage-service
          - alert-service
          - insight-service
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Check if service changed
        id: changed
        run: |
          if [ "${{ needs.detect-changes.outputs[matrix.service] }}" == "true" ]; then
            echo "changed=true" >> $GITHUB_OUTPUT
          else
            echo "changed=false" >> $GITHUB_OUTPUT
          fi

      - name: Configure AWS credentials
        if: steps.changed.outputs.changed == 'true'
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        if: steps.changed.outputs.changed == 'true'
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1

      - name: Extract metadata (tags, labels)
        if: steps.changed.outputs.changed == 'true'
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.ECR_REGISTRY }}/energy-tracker/${{ matrix.service }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        if: steps.changed.outputs.changed == 'true'
        uses: docker/build-push-action@v4
        with:
          context: ./${{ matrix.service }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Image digest
        if: steps.changed.outputs.changed == 'true'
        run: echo "Image pushed with digest: ${{ steps.docker_build.outputs.digest }}"

  scan-images:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    strategy:
      matrix:
        service:
          - user-service
          - device-service
          - ingestion-service
          - usage-service
          - alert-service
          - insight-service
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Get scan results
        run: |
          aws ecr describe-image-scan-findings \
            --repository-name energy-tracker/${{ matrix.service }} \
            --image-id imageTag=latest \
            --region ${{ env.AWS_REGION }} || echo "Scan not complete yet"
```

### Setup GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

- `AWS_ACCESS_KEY_ID` - IAM user access key with ECR permissions
- `AWS_SECRET_ACCESS_KEY` - IAM user secret key

**Required IAM Permissions:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:DescribeImages",
        "ecr:DescribeImageScanFindings"
      ],
      "Resource": "*"
    }
  ]
}
```

### Initial Manual Build & Push

For the first deployment, manually build and push images:

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_REGISTRY

# Build and push each service
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  echo "Building $service..."
  docker build -t $ECR_REGISTRY/energy-tracker/$service:latest ./$service
  docker push $ECR_REGISTRY/energy-tracker/$service:latest
done
```

### Configure Minikube to Pull from ECR

```bash
# Create AWS credentials secret in Kubernetes
kubectl create secret docker-registry ecr-credentials \
  --docker-server=$ECR_REGISTRY \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  --namespace energy-tracker

# Note: ECR tokens expire after 12 hours, consider using a token refresh strategy
# For long-running minikube: create a cronjob to refresh the secret
```

### Update Helm Values for ECR

**microservices-chart/values-minikube.yaml:**
```yaml
global:
  imageRegistry: "<account-id>.dkr.ecr.us-east-1.amazonaws.com/energy-tracker/"
  imagePullPolicy: Always  # Changed from IfNotPresent
  imagePullSecrets:
    - name: ecr-credentials

userService:
  image:
    repository: user-service  # Will be prefixed with imageRegistry
    tag: latest

# ... same pattern for other services
```

### Add imagePullSecrets to Deployment Templates

Update all deployment templates (user-service, device-service, etc.) to include:

```yaml
spec:
  template:
    spec:
      imagePullSecrets:
      {{- range .Values.global.imagePullSecrets }}
      - name: {{ .name }}
      {{- end }}
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.global.imageRegistry }}{{ .Values.userService.image.repository }}:{{ .Values.userService.image.tag }}"
```

### ECR Token Refresh CronJob (Optional)

Create `k8s/scripts/refresh-ecr-token.yaml`:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ecr-credentials-refresh
  namespace: energy-tracker
spec:
  schedule: "0 */8 * * *"  # Every 8 hours
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: ecr-credentials-refresher
          containers:
          - name: refresh
            image: amazon/aws-cli:latest
            command:
            - /bin/sh
            - -c
            - |
              kubectl delete secret ecr-credentials --ignore-not-found
              kubectl create secret docker-registry ecr-credentials \
                --docker-server=${ECR_REGISTRY} \
                --docker-username=AWS \
                --docker-password=$(aws ecr get-login-password --region ${AWS_REGION}) \
                --namespace energy-tracker
            env:
            - name: ECR_REGISTRY
              value: "<account-id>.dkr.ecr.us-east-1.amazonaws.com"
            - name: AWS_REGION
              value: "us-east-1"
          restartPolicy: OnFailure
```

### Verification

```bash
# Verify images in ECR
aws ecr list-images --repository-name energy-tracker/user-service --region us-east-1

# Test pull from minikube
kubectl run test-pull --image=$ECR_REGISTRY/energy-tracker/user-service:latest -n energy-tracker --rm -it -- /bin/sh

# Check GitHub Actions
# Push a commit to trigger the pipeline
git add .
git commit -m "test: trigger ECR build"
git push origin main
```

---

## PHASE 5: Microservices Migration

### Verify Images in ECR

```bash
# Ensure all images are pushed to ECR (from Phase 4)
export ECR_REGISTRY=$(aws ecr describe-repositories --region us-east-1 --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)

# Verify each service has images
for service in user-service device-service ingestion-service usage-service alert-service insight-service; do
  echo "Checking $service..."
  aws ecr describe-images --repository-name energy-tracker/$service --region us-east-1 | grep imageTag
done
```

### Configure Minikube for ECR Access

```bash
# Create ECR credentials secret (if not already created in Phase 4)
kubectl create secret docker-registry ecr-credentials \
  --docker-server=$ECR_REGISTRY \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  --namespace energy-tracker

# Verify secret
kubectl get secret ecr-credentials -n energy-tracker
```

### Create Global Configuration

**configmap-global.yaml:**
```yaml
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql.energy-tracker.svc.cluster.local:3306/energy_tracker"
  SPRING_KAFKA_BOOTSTRAP_SERVERS: "kafka.energy-tracker.svc.cluster.local:9092"
  INFLUX_URL: "http://influxdb.energy-tracker.svc.cluster.local:8086"
  INFLUX_ORG: "chieaid24"
  INFLUX_BUCKET: "usage-bucket"
  USER_SERVICE_URL: "http://user-service.energy-tracker.svc.cluster.local:8080/api/v1/user"
  DEVICE_SERVICE_URL: "http://device-service.energy-tracker.svc.cluster.local:8081/api/v1/device"
  USAGE_SERVICE_URL: "http://usage-service.energy-tracker.svc.cluster.local:8083/api/v1/usage"
  INGESTION_ENDPOINT: "http://ingestion-service.energy-tracker.svc.cluster.local:8082/api/v1/ingestion"
  SPRING_MAIL_HOST: "mailpit.energy-tracker.svc.cluster.local"
  SPRING_MAIL_PORT: "1025"
  SPRING_AI_OLLAMA_BASE_URL: "http://ollama.energy-tracker.svc.cluster.local:11434"
```

**secret-global.yaml:**
```yaml
stringData:
  MYSQL_USERNAME: "root"
  MYSQL_PASSWORD: "password"
  INFLUX_TOKEN: "my-token"
```

### Deployment Pattern (All Services)

```yaml
spec:
  initContainers:
  - name: wait-for-dependencies
    image: busybox:1.36
    command:
    - sh
    - -c
    - |
      until nc -z mysql.energy-tracker.svc.cluster.local 3306; do
        echo "Waiting for MySQL..."
        sleep 5
      done
  containers:
  - name: <service-name>
    image: <service-name>:latest
    imagePullPolicy: IfNotPresent
    envFrom:
    - configMapRef:
        name: microservices-config
    env:
    - name: SPRING_DATASOURCE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: microservices-secrets
          key: MYSQL_PASSWORD
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 808X
      initialDelaySeconds: 60
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 808X
      initialDelaySeconds: 30
```

### Service-Specific Config

**Ingestion Service:**
```yaml
# configmap
data:
  SIMULATION_REQUESTS_PER_INTERVAL: "1000"
  SIMULATION_INTERVAL_MS: "5000"
  SIMULATION_PARALLEL_THREADS: "10"
  SPRING_KAFKA_TEMPLATE_DEFAULT_TOPIC: "energy-usage"
```

**Usage Service:**
```yaml
env:
  - name: SPRING_KAFKA_CONSUMER_GROUP_ID
    value: "usage-service"
  - name: SPRING_KAFKA_TEMPLATE_DEFAULT_TOPIC
    value: "energy-alerts"
```

**Alert Service:**
```yaml
env:
  - name: SPRING_KAFKA_CONSUMER_GROUP_ID
    value: "alert-service"
```

**Insight Service:**
```yaml
env:
  - name: SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL
    value: "deepseek-r1"
  - name: SPRING_AI_OLLAMA_INIT_PULL_MODEL_STRATEGY
    value: "never"
```

### Microservices Ingress

```yaml
rules:
- host: energy-tracker.local
  paths:
  - path: /api/v1/user(/|$)(.*)
    backend: user-service:8080
  - path: /api/v1/device(/|$)(.*)
    backend: device-service:8081
  - path: /api/v1/ingestion(/|$)(.*)
    backend: ingestion-service:8082
  - path: /api/v1/usage(/|$)(.*)
    backend: usage-service:8083
  - path: /api/v1/alert(/|$)(.*)
    backend: alert-service:8084
  - path: /api/v1/insight(/|$)(.*)
    backend: insight-service:8085
```

### Deploy Microservices

```bash
helm install microservices ./k8s/helm-charts/microservices-chart \
  --namespace energy-tracker \
  --values ./k8s/helm-charts/microservices-chart/values.yaml \
  --values ./k8s/helm-charts/microservices-chart/values-minikube.yaml \
  --wait

# Verify
kubectl get pods -n energy-tracker
kubectl get svc -n energy-tracker
```

**Note:** For EKS deployment, use:
```bash
helm install microservices ./k8s/helm-charts/microservices-chart \
  --values ./k8s/helm-charts/microservices-chart/values.yaml \
  --values ./k8s/helm-charts/microservices-chart/values-eks.yaml \
  --namespace energy-tracker
```

---

## PHASE 6: Configure Ingress Access

### Setup DNS

```bash
# Add to /etc/hosts
MINIKUBE_IP=$(minikube ip)
echo "$MINIKUBE_IP energy-tracker.local" | sudo tee -a /etc/hosts

# Or run minikube tunnel (requires separate terminal)
minikube tunnel  # Keep running
```

### Test Access

```bash
# Infrastructure UIs
curl http://energy-tracker.local/kafka-ui
curl http://energy-tracker.local/mailpit
curl http://energy-tracker.local/influxdb

# Microservices APIs
curl http://energy-tracker.local/api/v1/user/actuator/health
curl http://energy-tracker.local/api/v1/device/actuator/health
```

---

## PHASE 7: Testing & Validation

### Test MySQL & Flyway

```bash
kubectl run mysql-client --rm -it --image=mysql:8.3.0 -n energy-tracker -- \
  mysql -h mysql.energy-tracker.svc.cluster.local -uroot -ppassword \
  -e "USE energy_tracker; SHOW TABLES;"

# Should show: user, device, alert, flyway_schema_history
```

### Test Kafka Topics

```bash
kubectl run kafka-test --rm -it --image=apache/kafka:latest -n energy-tracker -- \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka.energy-tracker.svc.cluster.local:9092 --list

# Or check Kafka-UI: http://energy-tracker.local/kafka-ui
```

### Create Test User & Device

```bash
# Create user
curl -X POST http://energy-tracker.local/api/v1/user \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@k8s.local",
    "alerting": true,
    "energyAlertingThreshold": 500.0
  }'

# Create device
curl -X POST http://energy-tracker.local/api/v1/device \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Device",
    "type": "REFRIGERATOR",
    "userId": 1
  }'
```

### Test Data Simulation

```bash
# Start simulation
curl -X POST http://energy-tracker.local/api/v1/ingestion/simulation/start

# Check status
curl http://energy-tracker.local/api/v1/ingestion/simulation/status

# Monitor logs
kubectl logs -f deployment/ingestion-service -n energy-tracker
kubectl logs -f deployment/usage-service -n energy-tracker
```

### Verify Data Flow

**Kafka:**
```bash
kubectl run kafka-consumer --rm -it --image=apache/kafka:latest -n energy-tracker -- \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka.energy-tracker.svc.cluster.local:9092 \
  --topic energy-usage \
  --max-messages 10
```

**InfluxDB:**
```bash
kubectl exec -n energy-tracker deployment/influxdb -- \
  influx query 'from(bucket:"usage-bucket") |> range(start:-10m) |> limit(n:10)' \
  --token my-token --org chieaid24
```

**Alerts:**
- Navigate to http://energy-tracker.local/mailpit
- Send high consumption event to trigger alert
- Verify email received in Mailpit UI

**AI Insights:**
```bash
curl -X POST http://energy-tracker.local/api/v1/insight/generate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "startDate": "'$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)'",
    "endDate": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'
```

---

## PHASE 8: Operational Tasks

### Common Commands

```bash
# View logs
kubectl logs -f deployment/user-service -n energy-tracker
kubectl logs deployment/usage-service -n energy-tracker --tail=100

# Port forward for debugging
kubectl port-forward svc/user-service 8080:8080 -n energy-tracker

# Execute commands
kubectl exec -it deployment/mysql -n energy-tracker -- mysql -uroot -ppassword

# Scale service
kubectl scale deployment user-service --replicas=3 -n energy-tracker

# Restart service
kubectl rollout restart deployment/user-service -n energy-tracker

# Update configuration
kubectl edit configmap microservices-config -n energy-tracker
kubectl rollout restart deployment/ingestion-service -n energy-tracker

# Check resource usage
kubectl top nodes
kubectl top pods -n energy-tracker

# Check all resources
kubectl get all -n energy-tracker
```

### Rebuild & Redeploy Service

**Option 1: Automatic via GitHub Actions (Recommended)**

```bash
# Make code changes
git add user-service/
git commit -m "feat: update user service"
git push origin main

# GitHub Actions will automatically:
# 1. Detect changes in user-service/
# 2. Build Docker image
# 3. Push to ECR with tags: latest, main, main-<sha>

# Wait for Actions to complete (check GitHub Actions tab)

# Then restart deployment to pull new image
kubectl rollout restart deployment/user-service -n energy-tracker
kubectl rollout status deployment/user-service -n energy-tracker
```

**Option 2: Manual Build & Push to ECR**

```bash
# Set ECR registry
export ECR_REGISTRY=$(aws ecr describe-repositories --region us-east-1 --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_REGISTRY

# Build and push
docker build -t $ECR_REGISTRY/energy-tracker/user-service:v2 ./user-service
docker push $ECR_REGISTRY/energy-tracker/user-service:v2

# Update deployment
kubectl set image deployment/user-service user-service=$ECR_REGISTRY/energy-tracker/user-service:v2 -n energy-tracker

# Or via Helm
helm upgrade microservices ./k8s/helm-charts/microservices-chart \
  --values ./k8s/helm-charts/microservices-chart/values.yaml \
  --values ./k8s/helm-charts/microservices-chart/values-minikube.yaml \
  --set userService.image.tag=v2 \
  -n energy-tracker

# Watch rollout
kubectl rollout status deployment/user-service -n energy-tracker
```

---

## PHASE 9: Backup & Persistence

### Create Backup Script

**k8s/scripts/backup-data.sh:**
```bash
#!/bin/bash
BACKUP_DIR="./k8s/backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

# MySQL backup
kubectl exec -n energy-tracker deployment/mysql -- \
  mysqldump -uroot -ppassword --all-databases > "$BACKUP_DIR/mysql-backup.sql"

# InfluxDB backup
kubectl exec -n energy-tracker deployment/influxdb -- \
  influx backup /tmp/influx-backup -t my-token
kubectl cp energy-tracker/$(kubectl get pod -l app=influxdb -n energy-tracker -o jsonpath='{.items[0].metadata.name}'):/tmp/influx-backup \
  "$BACKUP_DIR/influxdb-backup"

echo "Backup completed: $BACKUP_DIR"
```

### Storage Notes

- **Minikube storage:** Uses hostPath volumes (data persists across pod restarts, NOT minikube delete)
- **Location:** Inside minikube node at `/tmp/hostpath-provisioner/<pvc-name>`
- **Backup strategy:** Run backup script before `minikube delete` or major changes
- **Production:** Use network storage (NFS, Ceph) or cloud provider storage classes

---

## Critical Files to Create

### Must Create First (Priority Order)

1. **.github/workflows/build-and-push.yml** - CI/CD pipeline for ECR
2. **k8s/helm-charts/infrastructure-chart/Chart.yaml**
3. **k8s/helm-charts/infrastructure-chart/values.yaml**
4. **k8s/helm-charts/infrastructure-chart/values-minikube.yaml**
5. **k8s/helm-charts/infrastructure-chart/values-eks.yaml** (blank for now)
6. **k8s/helm-charts/infrastructure-chart/templates/mysql/configmap.yaml** (with FIXED init.sql)
7. **k8s/helm-charts/microservices-chart/Chart.yaml**
8. **k8s/helm-charts/microservices-chart/values.yaml**
9. **k8s/helm-charts/microservices-chart/values-minikube.yaml** (with ECR registry)
10. **k8s/helm-charts/microservices-chart/values-eks.yaml** (blank for now)
11. **k8s/helm-charts/microservices-chart/templates/configmap-global.yaml**
12. **k8s/helm-charts/microservices-chart/templates/secret-global.yaml**

### Then Create Infrastructure Templates

- mysql/{secret,pvc,deployment,service}.yaml
- kafka/{configmap,pvc,statefulset,service}.yaml
- influxdb/{secret,pvc,deployment,service}.yaml
- mailpit/{deployment,service}.yaml
- ollama/{pvc,deployment,service}.yaml
- kafka-ui/{deployment,service}.yaml
- infrastructure ingress.yaml

### Then Create Microservice Templates

- user-service/{deployment,service}.yaml
- device-service/{deployment,service}.yaml
- ingestion-service/{configmap,deployment,service}.yaml
- usage-service/{deployment,service}.yaml
- alert-service/{deployment,service}.yaml
- insight-service/{deployment,service}.yaml
- microservices ingress.yaml

---

## Troubleshooting

### Pod Won't Start

```bash
kubectl describe pod <pod-name> -n energy-tracker
kubectl logs <pod-name> -n energy-tracker --previous
kubectl get events -n energy-tracker --sort-by='.lastTimestamp'
```

**Common causes:**
- Insufficient resources → check `kubectl describe nodes`
- PVC not bound → check `kubectl get pvc -n energy-tracker`
- Image not found → verify `eval $(minikube docker-env)` was run
- Missing dependencies → check initContainer logs

### Service Not Reachable

```bash
# Check endpoints
kubectl get endpoints -n energy-tracker

# Test DNS
kubectl run dns-test --rm -it --image=busybox -n energy-tracker -- \
  nslookup user-service.energy-tracker.svc.cluster.local

# Test connectivity
kubectl run curl-test --rm -it --image=curlimages/curl -n energy-tracker -- \
  curl http://user-service.energy-tracker.svc.cluster.local:8080/actuator/health
```

### Ingress Not Working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress resource
kubectl describe ingress microservices-ingress -n energy-tracker

# Run minikube tunnel (in separate terminal)
minikube tunnel
```

### Data Lost

```bash
# Check PVC status
kubectl get pvc -n energy-tracker

# Restore from backup
./k8s/scripts/restore-data.sh <backup-dir>
```

---

## Cleanup

```bash
# Uninstall Helm releases
helm uninstall microservices -n energy-tracker
helm uninstall infrastructure -n energy-tracker

# Delete namespace
kubectl delete namespace energy-tracker

# Stop minikube
minikube stop

# Delete cluster (WARNING: loses all data)
minikube delete
```

---

## Success Criteria

✅ ECR repositories created for all 6 microservices
✅ GitHub Actions pipeline configured and pushing to ECR
✅ All container images available in ECR with `latest` tag
✅ Minikube configured with ECR credentials
✅ All infrastructure pods Running (MySQL, Kafka, InfluxDB, Mailpit, Ollama)
✅ All microservice pods Running (6 services) pulling from ECR
✅ All PVCs Bound
✅ Flyway migrations completed (user, device, alert tables exist)
✅ Kafka topics auto-created (energy-usage, energy-alerts)
✅ InfluxDB bucket created (usage-bucket)
✅ Can send ingestion events via API
✅ Usage data stored in InfluxDB
✅ Alerts trigger and send emails to Mailpit
✅ AI insights generated via Ollama
✅ All services accessible via ingress at energy-tracker.local
✅ Data persists across pod restarts
✅ Code push triggers automatic image rebuild in GitHub Actions

---

## Resource Requirements

**Minikube:** 6 CPUs, 12GB RAM, 50GB disk
**MySQL:** 512Mi-1Gi memory, 500m-1000m CPU, 10Gi storage
**Kafka:** 1Gi-2Gi memory, 1000m-2000m CPU, 10Gi storage
**InfluxDB:** 512Mi-1Gi memory, 500m-1000m CPU, 10Gi storage
**Ollama:** 4Gi-8Gi memory, 2000m-4000m CPU, 10Gi storage
**Microservices:** 256Mi-512Mi memory each, 500m-1000m CPU each

---

## Verification Script

**k8s/scripts/verify-migration.sh:**
```bash
#!/bin/bash
echo "Verifying Kubernetes migration..."

# Check all pods running
echo "Checking pods..."
kubectl get pods -n energy-tracker | grep -v Running && echo "ERROR: Not all pods running" || echo "✅ All pods running"

# Check PVCs bound
echo "Checking PVCs..."
kubectl get pvc -n energy-tracker | grep -v Bound && echo "ERROR: Not all PVCs bound" || echo "✅ All PVCs bound"

# Test ingress
echo "Testing ingress..."
curl -s http://energy-tracker.local/api/v1/user/actuator/health | grep -q UP && echo "✅ Ingress working" || echo "ERROR: Ingress not working"

# Check Flyway migrations
echo "Checking database..."
kubectl exec -n energy-tracker deployment/mysql -- \
  mysql -uroot -ppassword -e "USE energy_tracker; SHOW TABLES;" | grep -q "user" && \
  echo "✅ Database tables exist" || echo "ERROR: Database not initialized"

echo "Verification complete!"
```
