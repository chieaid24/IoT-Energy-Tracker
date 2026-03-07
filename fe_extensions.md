# IoT Energy Tracker — Frontend PRD (Next.js)

## Purpose
A minimal Next.js frontend that surfaces all backend capabilities: simulation control, user/device management, energy usage data, AI insights, and alerts.

**Not** a production UX — the goal is full visibility into backend functionality, including developer/simulation controls.

**Base URL**: `http://localhost/api/v1` (via nginx ingress + minikube tunnel)

---

## Stack
- **Next.js 14** (App Router, `src/` layout)
- **Tailwind CSS** for styling
- **shadcn/ui** for pre-built components (cards, tables, buttons, badges)
- **fetch()** in Server Components or client-side hooks — no extra state library needed for MVP

---

## MVP Features

### 1. Simulation Control Bar (global, top of every page)
**Why**: Simulation is the primary way to generate data. It must always be accessible.

- `GET /api/v1/ingestion/simulation/status` → show "Running" / "Stopped" badge
- `POST /api/v1/ingestion/simulation/start` → "Start Simulation" button
- `POST /api/v1/ingestion/simulation/stop` → "Stop Simulation" button
- Poll status every 5s while visible

**Component**: `<SimulationBar />` — sticky header strip with status badge + two buttons.

---

### 2. Dashboard Page (`/`)
**Why**: Entry point showing system-wide state at a glance.

- Total users: `GET /api/v1/user/total`
- Total devices: `GET /api/v1/device/total`
- Simulation status badge (reuse from bar)
- Quick-action buttons:
  - "Seed 5 Users" → `POST /api/v1/user/create/dummy?users=5`
  - "Seed 20 Devices" → `POST /api/v1/device/create/dummy?devices=20`
  - "Delete All Users" → `DELETE /api/v1/user/all`
  - "Delete All Devices" → `DELETE /api/v1/device/all`

**Layout**: 4 stat cards + action buttons row.

---

### 3. Users Page (`/users`)
**Why**: Show and control all users, including alert configuration.

- List all users: `GET /api/v1/user/ids` → then fetch each `GET /api/v1/user/{id}`
  - Display: name, email, address, alerting toggle, threshold (kWh)
- Per-user actions:
  - Edit threshold: `PUT /api/v1/user/{id}` (inline form with current value)
  - Toggle alerting: `PUT /api/v1/user/{id}` (checkbox/switch)
  - Delete: `DELETE /api/v1/user/{id}`
- Seed users button: `POST /api/v1/user/create/dummy?users=N` (input + button)

**Layout**: Table with inline edit row for threshold/alerting.

---

### 4. Devices Page (`/devices`)
**Why**: Show device inventory across users.

- List all devices by user: iterate user IDs, `GET /api/v1/device/user/{userId}`
- Display: device name, type, location, owner user ID
- Seed devices button: `POST /api/v1/device/create/dummy?devices=N`
- Delete device: `DELETE /api/v1/device/{id}`

**Layout**: Table grouped by user, with type badge (e.g., REFRIGERATOR, WASHER).

---

### 5. Energy Usage Page (`/usage`)
**Why**: Core backend output — shows aggregated energy consumption per device per user.

- User selector: dropdown populated from `GET /api/v1/user/ids`
- Days selector: 1 / 3 / 7 / 30 (default: 3)
- Fetch: `GET /api/v1/usage/{userId}?days=N`
- Display: table of devices with `energyConsumed` (kWh), sorted descending
- Show total kWh at the bottom

**Layout**: Dropdown + days selector → table + total row.

---

### 6. AI Insights Page (`/insights`)
**Why**: Showcases the Ollama LLM integration — the most impressive backend feature.

- User selector: dropdown
- Days selector (default: 3)
- Fetch: `GET /api/v1/insight/overview/{userId}?days=N`
- Display:
  - Total energy: `energyUsage` kWh
  - Confidence score: `confidence` (badge, color-coded: green ≥85, yellow ≥70, red <70)
  - AI tips: render `tips` as Markdown (use `react-markdown`)

**Layout**: Stats row (kWh + confidence) → Markdown block below. Include a "Regenerate" button that re-calls the endpoint.

---

## Page Structure Summary

```
/                    → Dashboard (stats + seeding + simulation)
/users               → User table (CRUD + alerting config)
/devices             → Device table (CRUD, grouped by user)
/usage               → Energy usage per device (table)
/insights            → AI insights (Markdown + confidence)
```

---

## Implementation Order (easiest to hardest)

1. Scaffold Next.js + Tailwind + shadcn
2. `<SimulationBar />` with status polling
3. Dashboard page (stat cards + seed buttons)
4. Users page (table + inline edit)
5. Devices page (table)
6. Usage page (user selector + table)
7. Insights page (user selector + markdown render)

---

## Stretch Goals

### A. Auto-refresh Usage While Simulating
- Poll `GET /api/v1/usage/{userId}?days=1` every 10s when simulation is running
- Show a "live" indicator next to the table
- No chart library needed — just refreshing numbers

### B. Bar Chart for Energy Usage
- Use `recharts` (lightweight, React-native)
- Horizontal bar chart: device name → kWh consumed
- Easy to add as an alternative view toggle to the usage table

### C. Alerts Log Page (`/alerts`)
- The alert-service currently has no REST endpoint (Kafka-only consumer)
- **Option 1**: Add a simple `GET /api/v1/alert/recent` endpoint to alert-service that stores and returns the last N alerts from an in-memory list or DB table
- **Option 2 (zero backend change)**: Embed Mailpit at `http://localhost:8025` in an iframe as a "poor man's alert log"
- Display if Option 1: userId, email, threshold, consumed, timestamp

### D. Embedded Infra UIs
- Mailpit iframe: `http://localhost:8025`
- Kafka UI iframe: `http://localhost:8080`
- InfluxDB iframe: `http://localhost:8086`
- Group under a `/infra` tab — zero backend work, instant visibility into the full stack

### E. Live Energy Chart (Time-Series)
- Poll the usage endpoint every few seconds while simulation runs
- Line chart with `recharts` showing total kWh over time for a selected user
- Most visually impressive demo feature

### F. Single-User Simulation Focus
- Add a user selector to the simulation bar
- Makes it easy to watch one specific user's usage rise in real time on the Usage page

### G. Manual Create Forms
- Form to create a single user with custom fields (name, email, address, threshold)
- Form to create a single device with custom fields (name, type, location, userId)
- More realistic than dummy-data-only endpoints

### H. Dark Mode
- Tailwind `dark:` variant + shadcn built-in dark theme support
- One-time config toggle in the nav header

---

## Notes
- All API calls go to `http://localhost/api/v1/...` — nginx ingress requires `minikube tunnel` running
- No auth on the backend — no login screen needed
- `react-markdown` for rendering LLM insight tips as formatted Markdown
- Recommended shadcn/ui components: `Card`, `Table`, `Badge`, `Button`, `Select`, `Switch`, `Input`
