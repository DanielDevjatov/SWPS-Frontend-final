const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { groth16, zKey } = require('snarkjs');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(express.json());
app.use(morgan('tiny'));
app.use(cors());

const presentations = [];
const verifications = [];

const zkeyPath =
  process.env.ZKP_ZKEY_PATH ||
  path.join(__dirname, '..', 'zkp', 'test_circuit', 'test_circuit.zkey');

let verificationKeyPromise = null;
async function getVerificationKey() {
  if (!verificationKeyPromise) {
    verificationKeyPromise = fs.promises
      .readFile(zkeyPath)
      .then((data) => zKey.exportVerificationKey(data));
  }
  return verificationKeyPromise;
}

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

function isNumber(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

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

app.get('/presentations', (req, res) => {
  res.json(presentations);
});

app.get('/verifications', (req, res) => {
  res.json(verifications);
});

const port = process.env.PORT || 8083;
if (require.main === module) {
  app.listen(port, () => {
    console.log(`TSO service listening on port ${port}`);
  });
}

module.exports = { verifyAggregatorPresentation };
