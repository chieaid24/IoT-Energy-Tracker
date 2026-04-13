# Shelly Integration

I use **Shelly Plug US Gen4** smart plugs to send real energy telemetry into the ingestion pipeline. Each plug runs a small script that periodically POSTs live power readings directly to `ingestion-service`.

## How It Works

The script (`iot-connect.js`) runs on the Shelly device itself using the built-in scripting engine. On a configurable interval, it reads the current switch status (power, voltage, current, energy, temperature) and POSTs it as JSON to the ingestion endpoint.

```
Shelly Plug (HTTP POST every 5s)  →  ingestion-service :8082  →  Kafka  →  usage-service
```

## Setup

### 1. Deploy the ingestion-service

Ensure `ingestion-service` is running and accessible from your local network (Docker Compose or Kubernetes with `minikube tunnel`).

The target endpoint is:
```
POST http://<your-server-ip>:8082/api/v1/ingestion/shelly/{deviceId}
```

### 2. Register your device

Create a device entry via `device-service` so the ingestion pipeline can associate readings with a known device ID. Note the numeric device ID returned.

### 3. Load the script onto the Shelly

1. Open the Shelly web UI (navigate to the plug's local IP in a browser).
2. Go to **Scripts** → **Create script**.
3. Paste the contents of `iot-connect.js`.
4. Update the two config values at the top of the file:

```js
let CONFIG = {
  endpoint: "http://<your-server-ip>:8082/api/v1/ingestion/shelly/<deviceId>",
  interval_sec: 5
};
```

5. **Save** and **Enable** the script.

### 4. Verify

Check that data is flowing:
- Kafka UI (`http://localhost:8070`) — messages appearing on the `energy-usage` topic.
- InfluxDB UI (`http://localhost:8072`) — readings written to `usage-bucket`.

## Payload

Each POST sends the following fields from the Shelly switch component:

| Field | Description |
|---|---|
| `apower` | Active power (W) |
| `voltage` | Voltage (V) |
| `current` | Current (A) |
| `aenergy` | Accumulated energy (Wh) |
| `temperature` | Device temperature (°C) |
