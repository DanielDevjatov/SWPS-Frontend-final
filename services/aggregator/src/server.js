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

// Section: In-memory storage for consents and presentations
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
  consents,
  presentations,
  aggregateConsents,
};


