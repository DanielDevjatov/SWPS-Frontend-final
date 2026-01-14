// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Imports (HTTP server, logging, ZKP helpers, IDs)
const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { groth16, zKey } = require('snarkjs');
const { v4: uuidv4 } = require('uuid');

// Section: Express app setup + middleware
const app = express();
app.use(express.json());
app.use(morgan('tiny'));
app.use(cors());

// Section: In-memory storage for presentations and verifications
const presentations = [];
const verifications = [];

// Section: ZKP artifact path (configurable via env)
const zkeyPath =
  process.env.ZKP_ZKEY_PATH ||
  path.join(__dirname, '..', 'zkp', 'test_circuit', 'test_circuit.zkey');
const tsoIssuer = process.env.TSO_ISSUER || 'tso-1';
const agentId = process.env.AGENT_ID || 'agent-1';
const agentUrl = process.env.AGENT_URL || 'http://agent:8081';
const aggregatorUrl = process.env.AGGREGATOR_URL || 'http://aggregator:8082';

// Section: Lazy-load the verification key from the zkey file
let verificationKeyPromise = null;
async function getVerificationKey() {
  if (!verificationKeyPromise) {
    verificationKeyPromise = fs.promises
      .readFile(zkeyPath)
      .then((data) => zKey.exportVerificationKey(data));
  }
  return verificationKeyPromise;
}

// Section: Health endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Section: Numeric validation helper
function isNumber(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

// Section: Validate prequalification issuance input
function validatePrequalInput(body) {
  if (!body || typeof body !== 'object') {
    throw new Error('Invalid request body');
  }
  const {
    deviceId,
    oemId,
    gridConnectionArea,
    validFrom,
    validTo,
    prequalificationType,
  } = body;

  if (!deviceId || !oemId || !gridConnectionArea || !prequalificationType) {
    throw new Error('Missing required fields');
  }
  if (!isNumber(validFrom) || !isNumber(validTo)) {
    throw new Error('validFrom and validTo must be numbers');
  }
  if (validFrom >= validTo) {
    throw new Error('validFrom must be before validTo');
  }
}

// Section: Build a PrequalificationCredential issued by TSO
function buildPrequalificationCredential(body) {
  validatePrequalInput(body);
  return {
    type: 'PrequalificationCredential',
    id: uuidv4(),
    issuer: tsoIssuer,
    holder: agentId,
    timestamp: Date.now(),
    payload: {
      deviceId: body.deviceId,
      oemId: body.oemId,
      gridConnectionArea: body.gridConnectionArea,
      validFrom: body.validFrom,
      validTo: body.validTo,
      prequalificationType: body.prequalificationType,
    },
  };
}

// Section: Validate + verify AggregatorPresentations (includes ZKP)
async function verifyAggregatorPresentation(presentation, opts = {}) {
  if (!presentation || presentation.type !== 'AggregatorPresentation') {
    return { status: 'invalid', reason: 'Invalid presentation type' };
  }

  const tw = presentation.timeWindow;
  if (!tw || !isNumber(tw.start) || !isNumber(tw.end) || tw.start >= tw.end) {
    return { status: 'invalid', reason: 'Invalid timeWindow' };
  }

  if (!isNumber(presentation.totalFlexKW) || presentation.totalFlexKW < 0) {
    return { status: 'invalid', reason: 'Invalid totalFlexKW' };
  }

  if (!isNumber(presentation.consentCount) || presentation.consentCount < 0) {
    return { status: 'invalid', reason: 'Invalid consentCount' };
  }

  if (!presentation.proof || !presentation.publicSignals) {
    return { status: 'invalid', reason: 'Missing proof or publicSignals' };
  }

  // Minimal plausibility: publicSignals[0] should match totalFlexKW
  const firstSignal = Array.isArray(presentation.publicSignals)
    ? presentation.publicSignals[0]
    : presentation.publicSignals['0'];
  if (firstSignal !== undefined && Number(firstSignal) !== presentation.totalFlexKW) {
    return { status: 'invalid', reason: 'publicSignals mismatch totalFlexKW' };
  }

  if (opts.skipZkp) {
    return { status: 'valid', reason: null };
  }

  try {
    const vKey = await getVerificationKey();
    const isValid = await groth16.verify(vKey, presentation.publicSignals, presentation.proof);
    return isValid ? { status: 'valid', reason: null } : { status: 'invalid', reason: 'Invalid proof' };
  } catch (err) {
    return { status: 'invalid', reason: `ZKP verification failed: ${err.message}` };
  }
}

// Section: Issue prequalification and push to Agent wallet
app.post('/prequalifications/issue', async (req, res) => {
  try {
    const credential = buildPrequalificationCredential(req.body);
    const agentResponse = await fetch(`${agentUrl.replace(/\/$/, '')}/wallet/ingest/prequal`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credential),
    });

    const agentText = await agentResponse.text();
    let parsedAgent = null;
    try {
      parsedAgent = JSON.parse(agentText);
    } catch (_) {
      parsedAgent = agentText || null;
    }

    if (!agentResponse.ok) {
      return res.status(502).json({
        code: 'agent_unreachable',
        message: 'Agent ingest failed',
        agentResponse: parsedAgent,
      });
    }

    const aggResponse = await fetch(`${aggregatorUrl.replace(/\/$/, '')}/ingest/prequal`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credential),
    });

    const aggText = await aggResponse.text();
    let parsedAgg = null;
    try {
      parsedAgg = JSON.parse(aggText);
    } catch (_) {
      parsedAgg = aggText || null;
    }

    if (!aggResponse.ok) {
      return res.status(502).json({
        code: 'aggregator_unreachable',
        message: 'Aggregator ingest failed',
        agentIngest: parsedAgent,
        aggregatorResponse: parsedAgg,
      });
    }

    res.status(201).json({
      status: 'issued',
      prequalId: credential.id,
      agentIngest: parsedAgent,
      aggregatorIngest: parsedAgg,
    });
  } catch (err) {
    res.status(400).json({ code: 'invalid_request', message: err.message });
  }
});

// Section: Ingest endpoint that verifies and stores presentations
app.post('/presentations/ingest', async (req, res) => {
  const incoming = req.body || {};
  const id = incoming.id || uuidv4();
  const presentation = { ...incoming, id };

  const result = await verifyAggregatorPresentation(presentation);

  presentations.push(presentation);
  verifications.push({
    verificationId: uuidv4(),
    presentationId: id,
    status: result.status,
    reason: result.reason || null,
    checkedAt: Date.now(),
  });

  const httpStatus = result.status === 'valid' ? 200 : 400;
  res.status(httpStatus).json({ status: result.status, reason: result.reason, id });
});

// Section: Verification-only endpoint (stores a verification record)
app.post('/verify/aggregator-presentation', async (req, res) => {
  const presentation = req.body || {};
  const result = await verifyAggregatorPresentation(presentation);
  const record = {
    verificationId: uuidv4(),
    presentationId: presentation.id || null,
    status: result.status,
    reason: result.reason || null,
    checkedAt: Date.now(),
  };
  if (presentation && presentation.id) {
    presentations.push(presentation);
  }
  verifications.push(record);
  res.status(result.status === 'valid' ? 200 : 400).json(record);
});

// Section: List stored presentations
app.get('/presentations', (req, res) => {
  res.json(presentations);
});

// Section: List verification records
app.get('/verifications', (req, res) => {
  res.json(verifications);
});

// Section: Service bootstrap
const port = process.env.PORT || 8083;
if (require.main === module) {
  app.listen(port, () => {
    console.log(`TSO service listening on port ${port}`);
  });
}

// Section: Public module API for tests
module.exports = { verifyAggregatorPresentation, buildPrequalificationCredential };


