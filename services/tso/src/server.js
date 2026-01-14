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
module.exports = { verifyAggregatorPresentation };


