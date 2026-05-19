<h1 align="left"> Home Energy Tracker</h1>


Ingests IoT data from your home devices. 

Stores, processes, and delivers real-time alerts and AI insights.

Built for real production workloads with AWS Elastic Kubernetes Service, microservices, and end-to-end observability.

> Note: live deployment is currently down to save on costs


[![Github Release](https://img.shields.io/github/v/release/chieaid24/energy-tracker)](https://github.com/chieaid24/energy-tracker/releases)


<p align="center">

  <img width="800" alt="IoT Telemetry System Design" src="https://github.com/user-attachments/assets/998807a5-7010-4095-b0aa-1fb773d4f4ec" />
</p>

## Technical Highlights

**Infrastructure**
- AWS resources are **100% Infrastructure as Code** with Terraform, deployed through Helm.
- **Cloud-native Kubernetes** with HPA autoscaling, self-healing, and rolling deployments.

**CI/CD**
- Automated **CI/CD pipeline** with **GitHub Actions** that builds -> tests -> deploys to EKS.

**Observability**
- Full **observability stack** with tracing, log aggregation, and metrics collected in Grafana.

**Data Layer**
- Scalable DB layer with **MySQL + read replicas** for relational data and **InfluxDB + Redis** for real-time analytics.

## Demo

<table>
  <thead>
    <tr>
      <th width="50%" align="center">User Flow</th>
      <th width="50%" align="center">Grafana Dashboards</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td rowspan="2" align="center">
        <video src="https://github.com/user-attachments/assets/2bcbd484-bce7-4f25-9c6c-77020a187b9e" width="100%"></video>
      </td>
      <td align="center">
        <img width="1916" height="838" alt="Grafana JVM Metrics" src="https://github.com/user-attachments/assets/3affd3c4-1aa6-499c-9e9a-19c0c9cd806d" />
      </td>
    </tr>
    <tr>
      <td align="center">
        <img width="1916" height="838" alt="Grafana Service Health Overview" src="https://github.com/user-attachments/assets/fbcfc237-cfe5-4313-b558-8d0a82fa640c" />
      </td>
    </tr>
  </tbody>
</table>



## Functional Overview
**Data Pipeline**
- Ingests **streamed power readings** from IoT devices into Kafka, writing consumption data into InfluxDB and enabling temporal aggregations and per-device statistics.
- Caches frequent queries in **Redis** to avoid bottlenecks with horizontally scaled microservices

**Alerts**
- Emits **real-time alerts** when usage exceeds configured limits, sending email notifications and persisting events in MySQL.

**AI Insights**
- Generates **sustainability recommendations,** contextualized by your recent usage, devices, time of day, location, and more.

**Dashboard and Auth**
- **Real-time dashboard** with live summary cards, energy charts, alert history, and AI insights panel.
- Handles auth with **Google OAuth and credential login**, issuing JWTs with stateless backend validation and middleware-enforced route protection.


## Tools Used
<table>
  <tr>
    <td><strong>Backend</strong></td>
    <td><img src="https://img.shields.io/badge/Java-%23ED8B00?style=for-the-badge&logo=data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iaXNvLTg4NTktMSI/Pg0KPCEtLSBVcGxvYWRlZCB0bzogU1ZHIFJlcG8sIHd3dy5zdmdyZXBvLmNvbSwgR2VuZXJhdG9yOiBTVkcgUmVwbyBNaXhlciBUb29scyAtLT4NCjxzdmcgaGVpZ2h0PSI4MDBweCIgd2lkdGg9IjgwMHB4IiB2ZXJzaW9uPSIxLjEiIGlkPSJDYXBhXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIA0KCSB2aWV3Qm94PSIwIDAgNTAyLjYzMiA1MDIuNjMyIiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxnPg0KCTxnPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTI0MC44NjQsMjY5Ljg5NGMwLDAtMjguMDItNTMuOTkyLTI2Ljk4NS05My40NDVjMC43NTUtMjguMTkzLDY0LjMyNC01Ni4wNjIsODkuMjgxLTk2LjUyOQ0KCQkJQzMyOC4wNzQsMzkuNDMxLDMwMC4wNTQsMCwzMDAuMDU0LDBzNi4yMzQsMjkuMDc3LTEwLjM3Niw1OS4xNDdjLTE2LjYwOSwzMC4xMTMtNzcuOTE0LDQ3Ljc3OS0xMDEuNzQ5LDk5LjY3OQ0KCQkJUzI0MC44NjQsMjY5Ljg5NCwyNDAuODY0LDI2OS44OTR6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzQ1Ljc0MSwxMDUuODY5YzAsMC05NS40OTQsMzYuMzQ3LTk1LjQ5NCw3Ny44NDljMCw0MS41NDUsMjUuOTI4LDU1LjAyNywzMC4xMTMsNjguNTA5DQoJCQljNC4xNDIsMTMuNTI1LTcuMjY5LDM2LjM0Ny03LjI2OSwzNi4zNDdzMzcuMzYxLTI1Ljk1LDMxLjEwNS01Ni4wNjJjLTYuMjM0LTMwLjExMy0zNS4yOS0zOS40NzUtMTguNjU5LTY5LjU0NA0KCQkJQzI5Ni42NDYsMTQyLjc5OSwzNDUuNzQxLDEwNS44NjksMzQ1Ljc0MSwxMDUuODY5eiIvPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTIzMC41MSwzMjQuNzQ4Yzg4LjI0Ni0zLjE0OSwxMjAuNDMtMzAuOTk3LDEyMC40My0zMC45OTcNCgkJCWMtNTcuMDc2LDE1LjU1My0yMDguNjU0LDE0LjUzOS0yMDkuNzExLDMuMTI4Yy0xLjAxNC0xMS40MTEsNDYuNzAxLTIwLjc3Myw0Ni43MDEtMjAuNzczcy03NC43MjEsMC04MC45NTUsMTguNjgNCgkJCUMxMDAuNzQsMzEzLjQ2NywxNDIuMzI4LDMyNy44MzMsMjMwLjUxLDMyNC43NDh6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzU4LjE4NywzNjguNDk0YzAsMCw4Ni4zNjktMTguNDIxLDc3LjgyNy02NS4zMzhjLTEwLjM1NC01Ny4xMTktNzAuNTgtMjQuOTM2LTcwLjU4LTI0LjkzNg0KCQkJczQyLjYwMiwwLDQ2LjcyMiwyNS45MjhDNDE2LjMyLDMzMC4wOTgsMzU4LjE4NywzNjguNDk0LDM1OC4xODcsMzY4LjQ5NHoiLz4NCgkJPHBhdGggc3R5bGU9ImZpbGw6I2ZmZmZmZjsiIGQ9Ik0zMTUuNjI4LDM0My42MDFjMCwwLTIxLjc2NSw1LjcxNi01NC4wMTMsOS4zNGMtNDMuMjI4LDQuODUzLTk1LjQ5NCwxLjAxNC05OS42NTctNi4yNTYNCgkJCWMtNC4wOTgtNy4yNjksNy4yNjktMTEuNDExLDcuMjY5LTExLjQxMWMtNTEuOTIxLDEyLjQ2OC0yMy41MTIsMzQuMjMzLDM3LjMzOSwzOC40MThjNTIuMTU4LDMuNTU5LDEyOS43OTEtMTUuNTc0LDEyOS43OTEtMTUuNTc0DQoJCQlMMzE1LjYyOCwzNDMuNjAxeiIvPg0KCQk8cGF0aCBzdHlsZT0iZmlsbDojZmZmZmZmOyIgZD0iTTE4MS43MzgsMzg4Ljk0M2MwLDAtMjMuNTU1LDAuNjY5LTI0LjkzNiwxMy4xMzdjLTEuMzU5LDEyLjM4MiwxNC40OTYsMjMuNTEyLDcyLjY1LDI2Ljk2NA0KCQkJYzU4LjEzMywzLjQ1MSw5OC45ODgtMTUuODk4LDk4Ljk4OC0xNS44OThsLTI2LjI5NS0xNS45NjJjMCwwLTE2LjYzMSwzLjQ5NC00Mi4yMzYsNi45NDYNCgkJCWMtMjUuNjI2LDMuNDczLTc4LjE3My0yLjc4My04MC4yNDMtNy41OTNDMTc3LjU1MywzOTEuNjgyLDE4MS43MzgsMzg4Ljk0MywxODEuNzM4LDM4OC45NDN6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNNDA3Ljk5NCw0NDUuMDA1YzguOTk1LTkuNzA3LTIuNzgzLTE3LjMyMS0yLjc4My0xNy4zMjFzNC4xNDIsNC44NTMtMS4zMzcsMTAuMzc2DQoJCQljLTUuNTQ0LDUuNTIyLTU2LjA4NCwxOS4zNDktMTM3LjA2MSwyMy41MTJjLTgwLjk1NSw0LjE2My0xNjguODU2LTcuNjE1LTE3MS42MzktMTcuOTkNCgkJCWMtMi42OTYtMTAuMzc2LDQ1LjAxOC0xOC42NTksNDUuMDE4LTE4LjY1OWMtNS41MjIsMC42OS03MS45NiwyLjA3MS03NC4wNzQsMjAuMDgyYy0yLjA3MSwxNy45NjgsMjkuMDU2LDMyLjUwNywxNTMuNjcsMzIuNTA3DQoJCQlDMzQ0LjMzOSw0NzcuNDkxLDM5OS4wNDIsNDU0LjY0Nyw0MDcuOTk0LDQ0NS4wMDV6Ii8+DQoJCTxwYXRoIHN0eWxlPSJmaWxsOiNmZmZmZmY7IiBkPSJNMzU5LjU2OCw0ODUuODE3Yy01NC42ODIsMTEuMDQ0LTIyMC43MzQsNC4wNzctMjIwLjczNCw0LjA3N3MxMDcuOTE5LDI1LjYyNiwyMzEuMTA5LDQuMTg1DQoJCQljNTguODg4LTEwLjI2OCw2Mi4zMTgtMzguNzYzLDYyLjMxOC0zOC43NjNTNDE0LjI1LDQ3NC43MDgsMzU5LjU2OCw0ODUuODE3eiIvPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCgk8Zz4NCgk8L2c+DQoJPGc+DQoJPC9nPg0KCTxnPg0KCTwvZz4NCjwvZz4NCjwvc3ZnPg==&logoColor=white" alt="Java" /> <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-%236DB33F?style=for-the-badge&logo=springboot&logoColor=%23FFFFFF" /> <img alt="Spring AI" src="https://img.shields.io/badge/Spring%20AI-%23d4aa00?style=for-the-badge&logo=spring&logoColor=%23FFFFFF"></td>
  </tr>
  <tr>
    <td><strong>Frontend</strong></td>
    <td><img alt="TypeScript" src="https://img.shields.io/badge/typescript-%233178C6?style=for-the-badge&logo=typescript&logoColor=%23FFFFFF"> <img alt="Next.js" src="https://img.shields.io/badge/Next.js-black?style=for-the-badge&logo=nextdotjs&logoColor=white" /> <img alt="Tailwind CSS" src="https://img.shields.io/badge/tailwind%20css-%2306B6D4?style=for-the-badge&logo=tailwindcss&logoColor=%23FFFFFF"></td>
  </tr>
  <tr>
    <td><strong>Data / Messaging</strong></td>
    <td><img alt="MySQL (RDS)" src="https://img.shields.io/badge/MySQL%20(RDS)-%234479A1?style=for-the-badge&logo=mysql&logoColor=%23FFFFFF"> <img alt="InfluxDB" src="https://img.shields.io/badge/InfluxDB-%2322ADF6?style=for-the-badge&logo=influxdb&logoColor=%23FFFFFF" /> <img alt="Flyway" src="https://img.shields.io/badge/Flyway-%23CC0200?style=for-the-badge&logo=flyway&logoColor=%23FFFFFF"> <img alt="Redis" src="https://img.shields.io/badge/Redis-%23FF4438?style=for-the-badge&logo=redis&logoColor=%23FFFFFF"> <img alt="Apache Kafka (MSK)" src="https://img.shields.io/badge/Apache%20Kafka%20(MSK)-%23231F20?style=for-the-badge&logo=apachekafka&logoColor=%23FFFFFF"></td>
  </tr>
  <tr>
    <td><strong>AI Inference</strong></td>
    <td><img src="https://img.shields.io/badge/AWS%20Bedrock-%2301a88d?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MDAiIGhlaWdodD0iODAwIiBmaWxsPSIjZmZmIiB2aWV3Qm94PSIwIDAgMzIgMzIiPjxwYXRoIGQ9Ik02LjU4NCA5LjAxYy0xLjM2IDAtMi43NC41My0yLjk3LjgyLS4wNi4xMi0uMiAxLjA5LjEzIDEuMDkuMTEgMCAuMTYuMDIuNDgtLjEzIDEuMi0uNDcgMS45Ni0uNDYgMi4wNy0uNDYgMS4zNS0uMTMgMi4xMy43OSAyLjAxIDEuOTh2LjdjLTEuMTQtLjI3LTEuNzktLjI4LTIuMTEtLjI4LTEuNjYtLjEtMy4xOTQuNzc2LTMuMTk0IDIuNyAwIDIuMTEgMS44ODMgMi41NiAyLjYxMyAyLjUzIDEuMDkuMDEgMi4xMy0uNDggMi44Mi0xLjMzLjU1IDEuMjMuOSAxLjE1LjkxIDEuMTUuMSAwIC4xOC0uMDQuMjYtLjA5bC41Ny0uNGMuMS0uMDYuMTgtLjE2LjE5LS4yOC0uMDEtLjI5LS41My0uNzQtLjQ5LTEuNzV2LTMuMTJhMy4xOCAzLjE4IDAgMCAwLS43OTktMi4zNSAzLjQyIDMuNDIgMCAwIDAtMi40OS0uNzhtMTkuMzczIDBjLTIgMC0zLjE1IDEuMjUtMy4xMiAyLjUyIDAgMS43NCAxLjc2IDIuMjkgMS45NiAyLjM1IDEuNjkuNTMgMS45Mi41NSAyLjM5Ljk1LjQuNDEuMzUgMS4yMS0uMjQgMS41Ni0uMTcuMS0uOS41NC0yLjU1LjItLjU1LS4xMS0uODQtLjI0LTEuMjktLjQzLS4xMi0uMDQtLjQtLjExLS40LjI2di40OWMwIC4yMy4xNC40NC4zNS41NCAxLjA1LjUzIDIuMzEuNTUgMi41OC41NS4wNCAwIDIuMzQuMDAxIDMuMTEtMS41NS4xNTgtLjMyLjU3LTEuNDktLjItMi40OS0uNjQtLjc1LTEuMTktLjgzLTIuODMtMS4zMy0uMTQtLjA0LTEuMzUtLjM1LTEuMzQtMS4yLS4wNi0xLjA5IDEuNDItMS4xNSAxLjczLTEuMTMgMS4yNS0uMDIgMS44Ny40NSAyLjIxLjQ4LjE1IDAgLjIyLS4wOS4yMi0uMjl2LS40NmEuNS41IDAgMCAwLS4wOS0uMzFjLS40LS41Mi0xLjkzLS43MS0yLjQ5LS43MW0tMTUuMTguMjVjLS4xMS4wMi0uMTkuMTMtLjE3LjI0LjAyLjEzLjA0LjI2LjA5LjM5bDIuMjQgNy4zOWMuMDUuMjQuMjEuNS41Ni40NmguODJjLjUuMDUuNTctLjQzLjU4LS40OGwxLjQ3LTYuMTYgMS40OSA2LjE3Yy4wMS4wNS4wOC41My41Ny40OGguODNjLjM2LjA0LjUzLS4yMi41OC0uNDYgMi41Mi04LjExIDIuMzUtNy41NiAyLjM3LTcuNjQuMDQtLjQyLS4yLS4zOS0uMjQtLjM4aC0uODljLS40NS0uMDUtLjU0LjM2LS41Ni40NmwtMS42NiA2LjQxLTEuNS02LjQxYy0uMDctLjQ5LS40Ny0uNDctLjU3LS40NmgtLjc3Yy0uNDQtLjA0LS41NS4zMS0uNTguNDZsLTEuNDkgNi4zMi0xLjYtNi4zMmMtLjA0LS4yLS4xNy0uNTEtLjU2LS40N3ptLTQuMjU0IDQuNjNjLjcyLjAxIDEuMzQyLjEyIDEuNzcyLjIyIDAgLjUuMDE4Ljc4LS4wOTIgMS4yMy0uMTQuNDgtLjc1OSAxLjM1LTIuMjE5IDEuMzctLjg0LjA0LTEuMzktLjYyLTEuMzQtMS4zNy0uMDUtMS4yIDEuMTktMS41IDEuODgtMS40NW0yMi41MTggNi4xMTJjLS45MzMuMDEzLTIuMDM1LjIyMi0yLjg3MS44MDktLjI1OC4xNzktLjIxMy40MjcuMDc0LjM5NC45NC0uMTEzIDMuMDMyLS4zNjcgMy40MDYuMTExcy0uNDE0IDIuNDUtLjc2MyAzLjMzMmMtLjEwOC4yNjMuMTIuMzcyLjM2MS4xNzIgMS41NjQtMS4zMSAxLjk3LTQuMDU2IDEuNjUtNC40NS0uMTYtLjE5OC0uOTI0LS4zODEtMS44NTctLjM2OG0tMjcuODI0IDFjLS4yMTguMDMtLjMxMi4zMDYtLjA4NC41MjVDNS4wNSAyNS4yMDEgMTAuMjI2IDI3IDE1Ljk3MyAyN2M0LjA5OSAwIDguODU3LTEuMzM3IDEyLjE0Mi0zLjg1Ny41NDMtLjQyLjA4LTEuMDQ3LS40NzYtLjgtMy42ODMgMS42MjYtNy42ODQgMi40MDktMTEuMzI1IDIuNDA5LTUuMzk2IDAtMTAuNjItMS4xMjctMTQuODQ1LTMuNjg2YS40LjQgMCAwIDAtLjI1Mi0uMDY0Ii8+PC9zdmc+&logoColor=white" alt="AWS Bedrock" /> <img alt="Ollama" src="https://img.shields.io/badge/Ollama-%23000000?style=for-the-badge&logo=ollama&logoColor=%23FFFFFF"></td>
  </tr>
  <tr>
    <td><strong>Observability</strong></td>
    <td><img alt="Prometheus" src="https://img.shields.io/badge/prometheus-%23E6522C?style=for-the-badge&logo=prometheus&logoColor=%23FFFFFF"> <img alt="Grafana" src="https://img.shields.io/badge/grafana-%23F46800?style=for-the-badge&logo=grafana&logoColor=%23FFFFFF"> <img alt="Loki" src="https://img.shields.io/badge/loki-%23D96800?style=for-the-badge&logoColor=%23FFFFFF"></td>
  </tr>
  <tr>
    <td><strong>Infrastructure</strong></td>
    <td><img alt="Kubernetes (EKS)" src="https://img.shields.io/badge/kubernetes%20(eks)-%23326CE5?style=for-the-badge&logo=kubernetes&logoColor=%23FFFFFF"> <img alt="Helm" src="https://img.shields.io/badge/helm-%230F1689?style=for-the-badge&logo=helm&logoColor=%23FFFFFF"> <img alt="Docker" src="https://img.shields.io/badge/Docker-%232496ED?style=for-the-badge&logo=docker&logoColor=%23FFFFFF" /> <img alt="Terraform" src="https://img.shields.io/badge/terraform-%23844FBA?style=for-the-badge&logo=terraform&logoColor=%23FFFFFF"> <img alt="GitHub Actions" src="https://img.shields.io/badge/GitHub%20Actions-%232088FF?style=for-the-badge&logo=githubactions&logoColor=%23FFFFFF"></td>
  </tr>
</table>


## AWS-Specific Architecture

Application runs on AWS EKS with managed RDS and MSK Serverless. 
- All 7 services autoscale with HPA (2-5 replicas) and survive node drains with PodDisruptionBudgets. 
- Infrastructure defined in Terraform, and deployments use the same Helm charts with EKS-specific value overlays.
- IRSA roles for pod level IAM, eliminating shared node roles and static credentials.

```
                Route53 (energy.aidanchien.com)
                                 │
                          NLB (internet-facing)
                                 │
                        ingress-nginx controller
                                 │
        ┌─────────────────────────────────────────────────┐
        │           │            │           │            │
    frontend   user-svc    device-svc   usage-svc    ... (7 svcs)
        │           │            │           │            │
        └─────── inter-service with ClusterIP DNS ────────┘
                     │                    │
                     ▼                    ▼
                 RDS MySQL           MSK Serverless

        insight-service ──► AWS Bedrock (Claude Haiku)
                            via cross-region inference profile (IRSA)
```

## Feature Details



<details>
<summary><strong>⚙️ Services</strong></summary>

<br>

| Service | Port | Persistence | What it does |
|---|---|---|---|
| user-service | 8080 | MySQL (primary + replica) | Handles registration, login, JWT issuance, and user profile CRUD. Flyway-managed schema, read replicas for query scaling. |
| device-service | 8081 | MySQL (primary + replica) | Manages IoT device registration and ownership. Validates users through inter-service REST calls. |
| ingestion-service | 8082 | - | Accepts energy readings via REST and publishes them to Kafka. Includes multi-threaded simulator for load testing. |
| usage-service | 8083 | InfluxDB, Redis | Consumes readings from Kafka, stores time-series data in InfluxDB, caches queries in Redis, and creates alert events when thresholds are exceeded. |
| alert-service | 8084 | MySQL (primary + replica) | Consumes alert events from Kafka, persists them, and sends email notifications with SMTP. |
| insight-service | 8085 | - | Polls recent usage data and sends it to an LLM (Ollama locally, Bedrock on EKS) to generate energy efficiency recommendations. |

</details>


<details>
<summary><strong>🎨 Frontend</strong></summary>

<br>

Next.js dashboard (App Router, TypeScript, Tailwind, shadcn/ui) served through nginx reverse proxy.

### Auth

- **Google OAuth 2.0 + credentials login** through NextAuth.js - backend validates tokens and issues HMAC-SHA signed JWTs.
- **Route protection** - middleware redirects unauthenticated users, stateless backend with no server-side sessions.

### Pages

| Route | Description |
|---|---|
| `/login` | Email/password + Google OAuth sign-in |
| `/register` | Account registration (auto-signs in on success) |
| `/dashboard` | Summary cards, energy bar chart, AI insights panel |
| `/dashboard/devices` | Device table, embedded InfluxDB explorer |
| `/dashboard/alerts` | Email alert inbox |

### Dashboard Components

- **Summary Cards** - device count, 7-day energy consumption, alert count (live polling).
- **Energy Chart** - per-device 7-day consumption bar chart using Recharts.
- **AI Insights Panel** - LLM-generated efficiency recommendations with confidence scores.

</details>

<details>
<summary><strong>🔍 Observability</strong></summary>

<br>

All 6 services are tracked across the three pillars, all aggregated in Grafana.

```
Spring Boot Services
  ├── /actuator/prometheus  ──────────────► Prometheus ─────► Grafana
  ├── OTLP traces (HTTP :4318) ──────────► Tempo ──────────► Grafana
  └── stdout JSON (ECS format) ─► Promtail ─► Loki ─────────► Grafana
```

- **Metrics** - Prometheus scrapes all 6 services every 15s (JVM, HTTP, Kafka producer/consumer).
- **Tracing** - OpenTelemetry traces pushed to Tempo, with trace context propagated across Kafka spans.
- **Logs** - Structured JSON to stdout, collected by Promtail into Loki. Log-to-trace correlation in Grafana.

### Provisioned Dashboards
| Dashboard | Description |
|---|---|
| Service Health Overview | HTTP request rates, error rates, latency per service |
| JVM Metrics | Heap, GC pause, thread count per service |
| Kafka Event Pipeline | Producer/consumer lag, throughput per topic |
| IoT Business Metrics | Device count, energy readings, alert frequency |

</details>

<details>
<summary><strong>🚀 CI/CD Pipeline</strong></summary>

<br>

### Overview

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

- **Change detection** - only rebuilds services with actual file changes.
- **Concurrency guard** - only one deploy-eks run at a time.
- **Claude PR reviews** - automatically reviewed for security and compliance by Claude.

All workflows use **GitHub OIDC**, so IAM role assumption (no stored AWS credentials).

</details>

<details>
<summary><strong>🛡️ Production Hardening</strong></summary>

### Production Hardening

| Feature | Configuration |
|---------|--------------|
| **HPA** | All 7 services autoscale: min 2, max 5 replicas, target 70% CPU |
| **PDB** | All 7 services: minAvailable=1 |
| **NetworkPolicies** | Default-deny + per-service ingress/egress allowlists |
| **CloudWatch Alarms** | RDS CPU >80%, connections >53/66 max, free storage <4 GiB, EKS failed nodes |
| **TLS throughout** | Let's Encrypt cert, TLS termination at ingress |
| **AWS Secrets Manager** | No plaintext in values files, synced using ExternalSecrets |
| **IRSA** | Pod-level IAM, ex. insight-service has Bedrock access only |

</details>

<details>
<summary><strong>📦 Deployments (Local / EKS)</strong></summary>

<br>

Three deployment targets: Docker Compose (local dev), Minikube (local k8s), and AWS EKS (production).

## Run With Docker

```bash
docker compose up -d                                    # core stack
docker compose -f docker-compose.observability.yml up -d  # observability stack
```

## Run With Kubernetes (Minikube)

> Requires: [Minikube](https://minikube.sigs.k8s.io/docs/start/), [Helm](https://helm.sh/docs/intro/install/), [kubectl](https://kubernetes.io/docs/tasks/tools/)

```bash
minikube start --gpus all --driver=docker --memory=8192 --cpus=6
minikube addons enable ingress

helm install infra ./k8s/charts/infra-chart
helm install observability ./k8s/charts/observability-chart
helm install microservices ./k8s/charts/microservices-chart

minikube tunnel  # in separate terminal, so we can expose services to localhost
```

## Run on AWS EKS (Production)

All infrastructure defined in Terraform; deployments use the same Helm charts with EKS-specific value overlays.

### AWS Resources (Terraform)

| Resource | Details |
|----------|---------|
| VPC | 3 public, 3 private, 3 DB subnets, NAT gateway |
| EKS | K8s 1.31, managed node group (t3.large × 3) |
| RDS MySQL | db.t3.medium, encrypted, 7-day backup, Performance Insights |
| MSK Serverless | IAM auth (SASL/SSL), topics: `energy-usage`, `energy-alerts` |
| Route53 + ACM | `energy.aidanchien.com`, Let's Encrypt TLS via cert-manager |
| Secrets Manager | 3 secrets synced to cluster using External Secrets Operator |
| IRSA | 7 pod-level IAM roles (ALB Controller, EBS CSI, ESO, ExternalDNS, cert-manager, insight-service, GHA deploy) |
| CloudWatch + SNS | 4 alarms (RDS CPU, connections, storage, EKS node health) |
| ECR Public | 8 image repos, SHA-tagged deploys |

### Deploy from Scratch

```bash
# 1. Provision infrastructure (~8 min)
cd terraform/envs/dev
terraform init && terraform apply

# 2. Configure kubectl
aws eks update-kubeconfig --name iot-tracker-dev --region us-east-1

# 3. Install cluster addons
./scripts/eks-bootstrap.sh

# 4. Deploy Helm charts
helm upgrade --install infra ./k8s/charts/infra-chart -f ./k8s/charts/infra-chart/values-eks.yaml
helm upgrade --install observability ./k8s/charts/observability-chart -f ./k8s/charts/observability-chart/values-eks.yaml
helm upgrade --install microservices ./k8s/charts/microservices-chart -f ./k8s/charts/microservices-chart/values-eks.yaml
```

### Teardown

```bash
helm uninstall microservices && helm uninstall observability && helm uninstall infra
cd terraform/envs/dev && terraform destroy
```

</details>
<details>
<summary><strong>💵 Cost</strong></summary>

<br>

### AWS Management Costs
| Component | Spec | Monthly |
|---|---|---|
| EKS control plane | K8s 1.31, standard support | $73.00 |
| EC2 nodes | 3× t3.large @ $0.0832/hr | $182.21 |
| RDS instance | db.t3.medium MySQL, single-AZ @ $0.068/hr | $49.64 |
| RDS storage | gp3, ~20 GB allocated @ $0.115/GB | $2.30 |
| MSK Serverless cluster | Flat $0.75/cluster-hr | $547.50 |
| MSK Serverless partitions | ~10 partitions @ $0.0015/hr | $10.95 |
| NAT Gateway | 1× @ $0.045/hr + data processing | ~$34 |
| NLB | Base @ $0.0225/hr + minimal LCU | ~$21 |
| EBS gp3 | 7 in-cluster PVCs + 3 node roots (~130 GB) | ~$11 |
| Secrets Manager | 3 secrets @ $0.40/mo + API calls | $1.20 |
| Route53 hosted zone | 1 zone | $0.50 |
| CloudWatch | 4 alarms (free tier), basic metrics/logs | ~$3 |
| ACM certificates | Public certs are free | $0 |
| Bedrock (Haiku 4.5) | Pay-per-token, low personal usage | ~$1–5 |
| Data transfer out | Internet egress | ~$1–3 |
| **Total** | | **~$940/mo** |

> This is very expensive :( so production environment is currently spun down. All infrastructure reproducible with `terraform apply` + `helm install` (see [Deploying to EKS](#run-on-aws-eks-production)).
</details>

---

## Real Usage

I connected the system to my own home using Shelly smartplugs. Check out the `/shelly` folder for how you can do it too!


---

## Project Layout

```
services/               # 6 Spring Boot microservices (user, device, ingestion, usage, alert, insight)
frontend/               # Next.js 16 (App Router, Tailwind v4, shadcn/ui)
shelly/                 # Shelly smart plug integration script + setup guide
observability/
  ├── prometheus/       # prometheus.yml - scrape configs for all services
  ├── grafana/          # Provisioned datasources (Prometheus, Loki, Tempo) + 4 dashboards
  ├── loki/             # loki.yaml - in-memory ring, TSDB storage
  ├── promtail/         # promtail.yaml - Docker SD, ECS JSON pipeline
  └── tempo/            # tempo.yaml - OTLP receivers, local storage
k8s/
  └── charts/
      ├── infra-chart/          # MySQL (primary + replica), Kafka, InfluxDB, Redis, Mailpit, Kafka UI, Ollama
      ├── observability-chart/  # Prometheus, Grafana, Loki, Promtail, Tempo
      └── microservices-chart/  # 6 services + frontend, shared ConfigMap/Secret/Ingress, HPA/PDB/NetworkPolicies
terraform/
  └── envs/dev/         # VPC, EKS, RDS, MSK Serverless, IAM (IRSA), Route53, ACM, CloudWatch + SNS alarms
scripts/
  └── eks-bootstrap.sh  # Installs EKS cluster addons (ALB Controller, ESO, cert-manager, etc.)
.github/workflows/
  ├── build-and-push.yml    # CI: detect changed services, build, push to ECR
  ├── deploy-eks.yml        # CD: helm upgrade infra -> observability -> microservices
  ├── build-test.yml        # Manual single-service rebuild from any branch
  └── claude-review.yml     # AI code review on PRs
docker-compose.yml                 # Core application stack
docker-compose.observability.yml   # Prometheus, Grafana, Loki, Promtail, Tempo
```
