// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Imports (HTTP server, logging, ZKP helpers, IDs)
const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const path = require('path');
const { groth16 } = require('snarkjs');
const { v4: uuidv4 } = require('uuid');

// Section: Express app setup + middleware
const app = express();
app.use(express.json());
app.use(morgan('tiny'));
app.use(cors());

// Section: In-memory storage for incoming credentials and presentations
const deviceSpecs = [];
const prequals = [];
const consents = [];
const presentations = [];

// Section: ZKP artifact paths (configurable via env)
const wasmPath =
  process.env.ZKP_WASM_PATH ||
  path.join(__dirname, '..', 'zkp', 'test_circuit', 'test_circuit.wasm');
const zkeyPath =
  process.env.ZKP_ZKEY_PATH ||
  path.join(__dirname, '..', 'zkp', 'test_circuit', 'test_circuit.zkey');

// Section: Health endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Section: Input validation for consent credentials
function validateConsent(cred) {
  if (!cred || cred.type !== 'ConsentCredential') {
    throw new Error('Expected ConsentCredential');
  }

  if (!cred.payload || typeof cred.payload !== 'object') {
    throw new Error('Missing consent payload');
  }

  const { timeWindow, maxFlexKW } = cred.payload;

  if (!timeWindow || typeof timeWindow.start !== 'number' || typeof timeWindow.end !== 'number') {
    throw new Error('timeWindow.start and timeWindow.end must be numbers');
  }

  if (timeWindow.start >= timeWindow.end) {
    throw new Error('timeWindow.start must be before timeWindow.end');
  }

  if (typeof maxFlexKW !== 'number' || maxFlexKW < 0) {
    throw new Error('maxFlexKW must be a non-negative number');
  }
}

// Section: Input validation for device specifications
function validateDeviceSpec(cred) {
  if (!cred || cred.type !== 'DeviceSpecificationsCredential') {
    throw new Error('Expected DeviceSpecificationsCredential');
  }
  if (!cred.payload || typeof cred.payload !== 'object') {
    throw new Error('Missing device specification payload');
  }
  const deviceId = cred.payload.deviceId || cred.payload.deviceID;
  if (!deviceId) {
    throw new Error('deviceId is required');
  }
}

// Section: Input validation for prequalification credentials
function validatePrequal(cred) {
  if (!cred || cred.type !== 'PrequalificationCredential') {
    throw new Error('Expected PrequalificationCredential');
  }
  if (!cred.payload || typeof cred.payload !== 'object') {
    throw new Error('Missing prequalification payload');
  }
  const deviceId = cred.payload.deviceId || cred.payload.deviceID;
  if (!deviceId) {
    throw new Error('deviceId is required');
  }
  if (typeof cred.payload.validFrom !== 'number' || typeof cred.payload.validTo !== 'number') {
    throw new Error('validFrom and validTo must be numbers');
  }
}

// Section: Normalize device specification payload and add metadata
function normalizeDeviceSpec(body) {
  validateDeviceSpec(body);
  const payload = { ...body.payload };
  payload.deviceId = payload.deviceId || payload.deviceID;
  return {
    type: 'DeviceSpecificationsCredential',
    id: body.id || uuidv4(),
    issuer: body.issuer || 'agent',
    holder: body.holder || 'aggregator',
    timestamp: body.timestamp || Date.now(),
    payload,
  };
}

// Section: Normalize prequalification payload and add metadata
function normalizePrequal(body) {
  validatePrequal(body);
  const payload = { ...body.payload };
  payload.deviceId = payload.deviceId || payload.deviceID;
  return {
    type: 'PrequalificationCredential',
    id: body.id || uuidv4(),
    issuer: body.issuer || 'tso',
    holder: body.holder || 'aggregator',
    timestamp: body.timestamp || Date.now(),
    payload,
  };
}

// Section: Normalize consent payload and add metadata
function normalizeConsent(body) {
  validateConsent(body);
  const payload = {
    ...body.payload,
    timeWindow: { ...body.payload.timeWindow },
  };
  return {
    type: 'ConsentCredential',
    id: body.id || uuidv4(),
    issuer: body.issuer || 'agent',
    holder: body.holder || 'aggregator',
    timestamp: body.timestamp || Date.now(),
    payload,
  };
}

// Section: Ingest a device specification credential
app.post('/ingest/device-spec', (req, res) => {
  try {
    const stored = normalizeDeviceSpec(req.body);
    const filtered = deviceSpecs.filter((d) => d.payload.deviceId !== stored.payload.deviceId);
    deviceSpecs.length = 0;
    deviceSpecs.push(...filtered, stored);
    res.status(201).json({ status: 'stored', id: stored.id });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Section: Ingest a prequalification credential
app.post('/ingest/prequal', (req, res) => {
  try {
    const stored = normalizePrequal(req.body);
    prequals.push(stored);
    res.status(201).json({ status: 'stored', id: stored.id });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Section: Ingest a single consent credential
app.post('/ingest/consent', (req, res) => {
  try {
    const stored = normalizeConsent(req.body);
    consents.push(stored);
    res.status(201).json({ status: 'stored', id: stored.id });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Section: List all device specifications
app.get('/device-specs', (req, res) => {
  res.json(deviceSpecs);
});

// Section: List all prequalifications
app.get('/prequals', (req, res) => {
  res.json(prequals);
});

// Section: List all stored consents
app.get('/consents', (req, res) => {
  res.json(consents);
});

// Section: Validate timeWindow inputs
function validateTimeWindow(timeWindow) {
  if (!timeWindow || typeof timeWindow.start !== 'number' || typeof timeWindow.end !== 'number') {
    throw new Error('timeWindow.start and timeWindow.end must be numbers');
  }

  if (timeWindow.start >= timeWindow.end) {
    throw new Error('timeWindow.start must be before timeWindow.end');
  }
}

// Section: Filter by exact timeWindow match (MVP behavior)
function consentMatchesWindow(consent, timeWindow) {
  const payloadWindow = consent.payload?.timeWindow;
  return payloadWindow && payloadWindow.start === timeWindow.start && payloadWindow.end === timeWindow.end;
}

// Section: Build per-device bundles across credential types
function buildDeviceBundles() {
  const deviceMap = new Map();

  deviceSpecs.forEach((spec) => {
    const deviceId = spec.payload.deviceId || spec.payload.deviceID;
    if (!deviceMap.has(deviceId)) {
      deviceMap.set(deviceId, { deviceId, deviceSpec: spec, prequals: [], consents: [] });
    } else {
      deviceMap.get(deviceId).deviceSpec = spec;
    }
  });

  prequals.forEach((p) => {
    const deviceId = p.payload.deviceId || p.payload.deviceID;
    if (!deviceMap.has(deviceId)) {
      deviceMap.set(deviceId, { deviceId, deviceSpec: null, prequals: [], consents: [] });
    }
    deviceMap.get(deviceId).prequals.push(p);
  });

  consents.forEach((c) => {
    const deviceId = c.payload?.deviceId || c.payload?.deviceID;
    if (!deviceMap.has(deviceId)) {
      deviceMap.set(deviceId, { deviceId, deviceSpec: null, prequals: [], consents: [] });
    }
    deviceMap.get(deviceId).consents.push(c);
  });

  return Array.from(deviceMap.values());
}

// Section: List device bundles (spec + prequals + consents)
app.get('/devices/bundles', (req, res) => {
  res.json(buildDeviceBundles());
});

// Section: Generate a demo ZKP proof for aggregated totalFlexKW
async function generateProof(totalFlexKW) {
  // Demo circuit: public a = totalFlexKW, private b = 1, circuit checks c = a*b.
  const input = { a: totalFlexKW.toString(), b: '1' };
  return groth16.fullProve(input, wasmPath, zkeyPath);
}

// Section: Aggregate consents into a single presentation
async function aggregateConsents(timeWindow) {
  let selectedConsents = consents;

  if (timeWindow) {
    validateTimeWindow(timeWindow);
    selectedConsents = consents.filter((c) => consentMatchesWindow(c, timeWindow));
  }

  if (!selectedConsents.length) {
    const err = new Error('No consents available for aggregation');
    err.statusCode = 400;
    throw err;
  }

  const totalFlexKW = selectedConsents.reduce((sum, c) => sum + (c.payload?.maxFlexKW || 0), 0);
  const consentCount = selectedConsents.length;

  const aggregateWindow =
    timeWindow ||
    selectedConsents.reduce(
      (acc, c) => {
        const payloadWindow = c.payload.timeWindow;
        return {
          start: Math.min(acc.start, payloadWindow.start),
          end: Math.max(acc.end, payloadWindow.end),
        };
      },
      {
        start: selectedConsents[0].payload.timeWindow.start,
        end: selectedConsents[0].payload.timeWindow.end,
      }
    );

  const proofResult = await generateProof(totalFlexKW);

  return {
    type: 'AggregatorPresentation',
    id: uuidv4(),
    createdAt: Date.now(),
    timeWindow: aggregateWindow,
    totalFlexKW,
    consentCount,
    status: 'aggregated',
    proof: proofResult.proof,
    publicSignals: proofResult.publicSignals,
  };
}

// Section: Aggregate endpoint (returns AggregatorPresentation)
app.post('/presentations/aggregate', async (req, res) => {
  try {
    const presentation = await aggregateConsents(req.body?.timeWindow);
    presentations.push(presentation);
    res.json(presentation);
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
  }
});

// Section: List all presentations
app.get('/presentations', (req, res) => {
  res.json(presentations);
});

// Section: Service bootstrap
const port = process.env.PORT || 8082;
if (require.main === module) {
  app.listen(port, () => {
    console.log(`Aggregator service listening on port ${port}`);
  });
}

// Section: Public module API for tests
module.exports = {
  normalizeConsent,
  normalizeDeviceSpec,
  normalizePrequal,
  deviceSpecs,
  prequals,
  consents,
  presentations,
  aggregateConsents,
};


