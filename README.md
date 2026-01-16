# SWPS Redispatch Prototype (Node + React)
This repo contains a minimal Redispatch prototype with three Node/Express
services (Agent, Aggregator, TSO) plus a React frontend for local use.


## Background (Short Theory)
Redispatch coordinates flexibility in the power grid to keep the system stable.
In this prototype, we split **stammdaten** (static device/OEM data) from
**bewegungsdaten** (dynamic consents and aggregations). The **Agent** manages
devices and creates consents, the **Aggregator** collects and aggregates them,
and the **TSO** authorizes participation by issuing prequalifications and
verifies aggregated results. This mirrors the DEER concept of clear roles:
the TSO is the issuer of prequalification (authorization), while the Agent is
the holder/producer of operational consents. The ZKP step is a demo-only proof
to show how cryptographic verification could be integrated without turning the
prototype into a full SSI/PKI system.

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

## Project Structure (Quick Overview)
- `services/agent/` - Wallet ingest + policy checks + consent creation/push
- `services/aggregator/` - Credential ingest + aggregation + per-device bundles
- `services/tso/` - Prequalification issuance + presentation verification
- `frontend/` - React UI (Agent/Aggregator/TSO screens)
- `docker-compose.yml` - Local orchestration for the three services
## Process Overview (End-to-End)
1) **Seed** devices (Agent) so the UI has OEM + device data.
2) **Issue Prequalification** (TSO) to authorize a device/timeWindow.
3) **Create & Push Consent** (Agent) which sends a Consent to Aggregator.
4) **Aggregate** consents (Aggregator) into a presentation + ZKP proof.
5) **Verify** presentation (TSO) and store the verification record.

## API Overview (by Service)
### Agent (8081)
- `GET /health` → `{ "status": "ok" }`
- `POST /admin/seed-one-oem-five-devices` → seeds OEM + 5 devices + whitelist
- `POST /wallet/ingest/device-spec` (DeviceSpecificationsCredential) → `{ status, id }`
- `POST /wallet/ingest/prequal` (PrequalificationCredential, **issuer must be tso-1**) → `{ status, id }`
- `GET /wallet/device-specs` → `[DeviceSpecificationsCredential]`
- `GET /wallet/oems` → `[OEMCredential]`
- `GET /wallet/devices?oemId=...` → `[DeviceSpecificationsCredential]`
- `GET /wallet/devices/:deviceId` → `DeviceSpecificationsCredential`
- `GET /admin/whitelist` → `["dev-1","dev-2",...]`
- `POST /issue/consent` → `ConsentCredential`
- `POST /push/consent-to-aggregator` → `{ status:"sent", aggregatorResponse:{...} }`
Example: push consent
```bash
curl -X POST http://localhost:8081/push/consent-to-aggregator \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"timeWindow\":{\"start\":100,\"end\":200},\"maxFlexKW\":2}"
```
### Aggregator (8082)
- `GET /health` → `{ "status": "ok" }`
- `POST /ingest/device-spec` (DeviceSpecificationsCredential) → `{ status, id }`
- `POST /ingest/prequal` (PrequalificationCredential) → `{ status, id }`
- `POST /ingest/consent` (ConsentCredential) → `{ status, id }`
- `GET /device-specs` → `[DeviceSpecificationsCredential]`
- `GET /prequals` → `[PrequalificationCredential]`
- `GET /consents` → `[ConsentCredential]`
- `GET /devices/bundles` → `{ deviceId, deviceSpec, prequals, consents }[]`
- `POST /presentations/aggregate` → `AggregatorPresentation`
- `GET /presentations` → `[AggregatorPresentation]`
### TSO (8083)
- `GET /health` → `{ "status": "ok" }`
- `POST /prequalifications/issue` → issues + pushes to Agent + Aggregator
- `POST /verify/aggregator-presentation` → `VerificationRecord`
- `POST /presentations/ingest` → `{ status, reason, id }`
- `GET /presentations` → `[AggregatorPresentation]`
- `GET /verifications` → `[VerificationRecord]`
Example: issue prequalification
```bash
curl -X POST http://localhost:8083/prequalifications/issue \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"dev-1\",\"oemId\":\"oem-1\",\"gridConnectionArea\":\"area1\",\"validFrom\":0,\"validTo\":1000,\"prequalificationType\":\"typeA\"}"
```
## Data Models (Core Objects)
### OEMCredential
```json
{
  "type": "OEMCredential",
  "id": "uuid",
  "issuer": "agent-1",
  "holder": "agent-1",
  "timestamp": 0,
  "payload": {
    "oemId": "oem-1",
    "name": "OEM ThermoGrid GmbH",
    "status": "active"
  }
}
```
### DeviceSpecificationsCredential
```json
{
  "type": "DeviceSpecificationsCredential",
  "id": "uuid",
  "issuer": "agent-1",
  "holder": "agent-1",
  "timestamp": 0,
  "payload": {
    "deviceId": "dev-1",
    "deviceName": "Industrial Boiler A (Hall 1)",
    "oemId": "oem-1",
    "gridConnectionArea": "area1",
    "ratedPowerKW": 120,
    "availableFlexKW": 6,
    "maxFlexCapKW": 4,
    "minFlexCapKW": 0,
    "deviceType": "Boiler",
    "location": "Regensburg"
  }
}
```
### PrequalificationCredential
```json
{
  "type": "PrequalificationCredential",
  "id": "uuid",
  "issuer": "tso-1",
  "holder": "agent-1",
  "timestamp": 0,
  "payload": {
    "deviceId": "dev-1",
    "oemId": "oem-1",
    "gridConnectionArea": "area1",
    "validFrom": 0,
    "validTo": 1000,
    "prequalificationType": "typeA"
  }
}
```
### ConsentCredential
```json
{
  "type": "ConsentCredential",
  "id": "uuid",
  "issuer": "agent-1",
  "holder": "http://aggregator:8082",
  "timestamp": 0,
  "payload": {
    "deviceId": "dev-1",
    "oemId": "oem-1",
    "timeWindow": { "start": 100, "end": 200 },
    "maxFlexKW": 2,
    "consentGivenToAggregator": true
  }
}
```
### AggregatorPresentation
```json
{
  "type": "AggregatorPresentation",
  "id": "uuid",
  "createdAt": 0,
  "timeWindow": { "start": 100, "end": 200 },
  "totalFlexKW": 2,
  "consentCount": 1,
  "status": "aggregated",
  "proof": { "pi_a": [], "pi_b": [], "pi_c": [] },
  "publicSignals": [2, 2]
}
```
### VerificationRecord
```json
{
  "verificationId": "uuid",
  "presentationId": "uuid",
  "status": "valid",
  "reason": null,
  "checkedAt": 0
}
```
## Policy & Validation Rules (Agent)
- **Device must exist** → else `404 unknown_device`
- **Device must be whitelisted** → `403 device_not_trusted`
- **Prequalification must exist + cover timeWindow** → `403 no valid prequalification`
- **availableFlexKW > 0** → `409 no_flex_available`
- **maxFlexKW > 0** → `422 invalid_maxFlexKW`
- **maxFlexKW <= availableFlexKW** → `409 maxFlex_exceeds_available`
- **maxFlexKW <= maxFlexCapKW** → `422 maxFlex_exceeds_device_cap`
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
- `frontend/` - React


