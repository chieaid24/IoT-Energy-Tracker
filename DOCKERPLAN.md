Phase 1: Inventory + Decisions

Identify the 6 services and their build systems (Gradle/Maven), Java versions, and default ports.
List config inputs: required env vars, application.properties overrides, profiles, and any secrets.
Map service dependencies (which service calls which, databases, message queues).
Decide on base images (e.g., eclipse-temurin:17-jre for runtime, maven:3.9-eclipse-temurin-17 for build).
Confirm whether you want single docker-compose.yml or multi-env (e.g., docker-compose.yml + docker-compose.dev.yml).
Phase 2: Per‑Service Dockerfiles

For each service, add:
Dockerfile (multi-stage build: build JAR, then run JAR).
.dockerignore (ignore target/, .idea/, .git/, etc.).
Standardize entrypoints (e.g., java -jar app.jar), and optionally add HEALTHCHECK.
If using Spring Boot, ensure SERVER_PORT or server.port can be overridden via env vars.
Phase 3: Compose Orchestration

Define docker-compose.yml with:
A service per app (names, build context, ports, env, depends_on).
A shared network.
Volumes for any stateful dependencies (DBs, message brokers).
Inject inter-service URLs (e.g., usage-service (line 8080)).
Optionally add docker-compose.dev.yml for live reloading or local overrides.
Phase 4: Verification + Docs

Build: docker compose build (or docker-compose build).
Run: docker compose up.
Verify each service health endpoint.
Add a short README section and optional scripts (e.g., dev-up.sh).


# Phase