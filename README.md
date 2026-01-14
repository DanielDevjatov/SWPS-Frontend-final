<!-- Summary (FinalFinal): Added code to *. This file explains how to run the Node/React prototype. -->
# SWPS Redispatch Prototype (Node + React)

This repo contains a minimal Redispatch prototype with three Node/Express
services (Agent, Aggregator, TSO) plus a React frontend for local use.

## Prerequisites

- Docker Desktop
- Node.js 18+ and npm (for frontend dev)

## Quick Start (Backend)

From the repo root:
```bash
docker compose up --build
```

Services:
- Agent: http://localhost:8081
- Aggregator: http://localhost:8082
- TSO: http://localhost:8083

## Quick Start (Frontend)

```bash
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

## Seed Demo Data

Seed one OEM and five devices (with whitelist + prequals for demo use):
```bash
curl -X POST http://localhost:8081/admin/seed-one-oem-five-devices
```

## Health Checks

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

## End-to-End Demo (API)

1) Push consent from Agent to Aggregator:
```bash
curl -X POST http://localhost:8081/push/consent-to-aggregator \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"timeWindow\":{\"start\":100,\"end\":200},\"maxFlexKW\":2}"
```

2) Aggregate in Aggregator:
```bash
curl -X POST http://localhost:8082/presentations/aggregate \
  -H "Content-Type: application/json" \
  -d "{\"timeWindow\":{\"start\":100,\"end\":200}}"
```

3) Verify in TSO:
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

