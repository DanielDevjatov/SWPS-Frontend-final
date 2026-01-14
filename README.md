<!-- Summary (FinalFinal): Added code to *. This file explains how to run the Node/React prototype. -->
# SWPS Redispatch Prototype (Node + React)

This repo contains a minimal Redispatch prototype with three Node/Express
services (Agent, Aggregator, TSO) plus a React frontend for local use.

## Fresh Setup (Step by Step)

### Step 0: Install prerequisites
- Docker Desktop
- Node.js 18+ and npm (for the frontend)

### Step 1: Start the backend (Docker)
From the repo root:
```powershell
docker compose up --build
```

Backend URLs:
- Agent: http://localhost:8081
- Aggregator: http://localhost:8082
- TSO: http://localhost:8083

### Step 2: Start the frontend (React)
Open a new terminal:
```powershell
cd "frontend"
npm install
npm start
```

Frontend URL:
- http://localhost:3000

Frontend env (already in `frontend/.env`):
- `REACT_APP_API_BASE=http://localhost:8081`
- `REACT_APP_AGG_BASE=http://localhost:8082`
- `REACT_APP_TSO_BASE=http://localhost:8083`

Note: These `localhost` URLs are correct for local dev.
If the frontend runs in Docker, use service hostnames or a reverse proxy.

## UI Demo (No Terminal Required)

Once the frontend is open:
1) Go to **Agent**.
2) Click **Seed OEM & Devices**.
3) Select a device and click **Request Prequalification**.
4) Enter maxFlex/timeWindow and click **Create & Push Consent**.
5) Go to **Aggregator** and click **Create Aggregated Presentation**.
6) Go to **TSO** and click **Verify**.

If a device has no prequal, the consent will be blocked (expected).

## Seed Demo Data (API)

Seed one OEM and five devices (whitelist only; prequals are issued by TSO):
```bash
curl -X POST http://localhost:8081/admin/seed-one-oem-five-devices
```

Issue a prequalification from the TSO (stored in the Agent wallet):
```bash
curl -X POST http://localhost:8083/prequalifications/issue \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"oemId\":\"oem-1\",\"gridConnectionArea\":\"area1\",\"validFrom\":0,\"validTo\":1000,\"prequalificationType\":\"typeA\"}"
```

## Health Checks

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

## End-to-End Demo (API)

1) Issue prequalification from TSO:
```bash
curl -X POST http://localhost:8083/prequalifications/issue \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"oemId\":\"oem-1\",\"gridConnectionArea\":\"area1\",\"validFrom\":0,\"validTo\":1000,\"prequalificationType\":\"typeA\"}"
```

2) Push consent from Agent to Aggregator:
```bash
curl -X POST http://localhost:8081/push/consent-to-aggregator \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"timeWindow\":{\"start\":100,\"end\":200},\"maxFlexKW\":2}"
```

3) Aggregate in Aggregator:
```bash
curl -X POST http://localhost:8082/presentations/aggregate \
  -H "Content-Type: application/json" \
  -d "{\"timeWindow\":{\"start\":100,\"end\":200}}"
```

4) Verify in TSO:
```bash
curl -X POST http://localhost:8083/verify/aggregator-presentation \
  -H "Content-Type: application/json" \
  -d "<paste AggregatorPresentation JSON>"
```

## Project Structure

- `services/agent/` - Wallet ingest + policy checks + consent push
- `services/aggregator/` - Consent ingestion + aggregation + demo ZKP proof
- `services/tso/` - Verify presentation + store verification records
- `frontend/` - React UI

## Legacy Kotlin Wallet (Not Used)

This repo still contains a Kotlin/JS wallet module under `wallet/`.
It is not used by the current Node/React prototype.

