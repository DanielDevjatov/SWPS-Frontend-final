// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const { normalizePrequalification, selectPrequalification, validationError } = require('./prequal');
const { normalizeDeviceSpec } = require('./device');
const { buildSeedOneOemFiveDevices } = require('./seed');

// Section: Express app setup
const app = express();
app.use(express.json());
app.use(morgan('tiny'));
app.use(cors());

// Section: In-memory state (wallet contents + last consent)
const state = {
  deviceSpecs: [],
  prequals: [],
  oems: [],
  lastConsent: null,
};

// Section: Runtime config and whitelist
// Device whitelist (deviceIds)
const initialDeviceWhitelist = (process.env.DEVICE_WHITELIST || '').split(',').map((s) => s.trim()).filter(Boolean);
const deviceWhitelist = new Set(initialDeviceWhitelist);

// Section: Service identity and default URLs
const agentId = process.env.AGENT_ID || 'agent-1';
const defaultAggregatorUrl = process.env.AGGREGATOR_URL || 'http://localhost:8082';
const tsoIssuer = process.env.TSO_ISSUER || 'tso-1';

// Section: Error helper used by policy checks
function validationErrorWithStatus(message, statusCode = 400, extra = {}) {
  const err = validationError(message);
  err.statusCode = statusCode;
  Object.assign(err, extra);
  if (Object.keys(extra || {}).length) {
    err.extra = extra;
  }
  return err;
}

// Section: Health endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Section: OEM storage (validate + upsert by oemId)
function storeOem(body) {
  if (!body || body.type !== 'OEMCredential') {
    throw validationError('Expected credential type OEMCredential');
  }
  if (!body.payload || typeof body.payload !== 'object') {
    throw validationError('Missing credential payload');
  }
  const payload = { ...body.payload };
  if (!payload.oemId) {
    throw validationError('oemId is required');
  }
  if (!payload.name) {
    throw validationError('name is required');
  }
  if (!['active', 'inactive'].includes(payload.status)) {
    throw validationError('status must be active|inactive');
  }
  const stored = {
    type: 'OEMCredential',
    id: body.id || uuidv4(),
    issuer: body.issuer || agentId,
    holder: body.holder || agentId,
    timestamp: body.timestamp || Date.now(),
    payload,
  };
  state.oems = state.oems.filter((o) => o.payload.oemId !== payload.oemId);
  state.oems.push(stored);
  return stored;
}

// Section: Device storage (validate + upsert by deviceId)
function storeDeviceSpec(body) {
  const stored = normalizeDeviceSpec(body, { issuer: agentId, holder: agentId });
  state.deviceSpecs = state.deviceSpecs.filter((d) => d.payload.deviceId !== stored.payload.deviceId);
  state.deviceSpecs.push(stored);
  return stored;
}

// Section: Push device specs to the Aggregator for cross-credential linking
async function pushDeviceSpecToAggregator(deviceSpec, aggregatorUrl = defaultAggregatorUrl) {
  const target = (aggregatorUrl || '').replace(/\/$/, '');
  const response = await fetch(`${target}/ingest/device-spec`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(deviceSpec),
  });
  const responseText = await response.text();
  let parsedResponse = null;
  try {
    parsedResponse = JSON.parse(responseText);
  } catch (_) {
    parsedResponse = responseText || null;
  }
  return { ok: response.ok, status: response.status, body: parsedResponse };
}

// Section: Prequalification storage (normalized credential)
function storePrequalification(body) {
  if (!body?.issuer || body.issuer !== tsoIssuer) {
    throw validationErrorWithStatus('invalid_issuer', 403, {
      code: 'invalid_issuer',
      message: `Prequal must be issued by ${tsoIssuer}`,
    });
  }
  const stored = normalizePrequalification(body, {
    deviceID: body.payload?.deviceId || body.payload?.deviceID,
    oemId: body.payload?.oemId,
    issuer: tsoIssuer,
    holder: agentId,
    id: uuidv4(),
  });
  state.prequals.push(stored);
  return stored;
}

// Section: Wallet ingest endpoints
app.post('/wallet/ingest/oem', (req, res) => {
  try {
    const stored = storeOem(req.body);
    res.status(201).json({ status: 'stored', id: stored.id });
  } catch (err) {
    res.status(err.statusCode || 400).json({ error: err.message });
  }
});

app.post('/wallet/ingest/device-spec', (req, res) => {
  try {
    const stored = storeDeviceSpec(req.body);
    pushDeviceSpecToAggregator(stored)
      .then((aggResponse) => {
        if (!aggResponse.ok) {
          return res.status(502).json({
            status: 'stored',
            id: stored.id,
            error: 'Aggregator ingest failed',
            aggregatorResponse: aggResponse.body,
          });
        }
        return res.status(201).json({ status: 'stored', id: stored.id, aggregatorResponse: aggResponse.body });
      })
      .catch((err) =>
        res.status(502).json({ status: 'stored', id: stored.id, error: 'Aggregator unreachable', reason: err.message })
      );
  } catch (err) {
    res.status(err.statusCode || 400).json({ error: err.message });
  }
});

app.post('/wallet/ingest/prequal', (req, res) => {
  try {
    const stored = storePrequalification(req.body);
    res.status(201).json({ status: 'stored', id: stored.id });
  } catch (err) {
    res.status(err.statusCode || 400).json({ error: err.message });
  }
});

// Section: Wallet read endpoints
app.get('/wallet/device-specs', (req, res) => {
  res.json(state.deviceSpecs);
});

app.get('/wallet/oems', (req, res) => {
  res.json(state.oems);
});

app.get('/wallet/devices', (req, res) => {
  const { oemId } = req.query || {};
  const devices = oemId
    ? state.deviceSpecs.filter((d) => d.payload.oemId === oemId)
    : state.deviceSpecs;
  res.json(devices);
});

app.get('/wallet/devices/:deviceId', (req, res) => {
  const device = state.deviceSpecs.find((d) => d.payload.deviceId === req.params.deviceId);
  if (!device) {
    return res.status(404).json({ error: 'unknown_device' });
  }
  res.json(device);
});

app.get('/admin/whitelist', (req, res) => {
  res.json(Array.from(deviceWhitelist));
});

// Section: TimeWindow validation
function validateTimeWindow(timeWindow) {
  if (!timeWindow || typeof timeWindow.start !== 'number' || typeof timeWindow.end !== 'number') {
    throw validationError('timeWindow.start and timeWindow.end must be numbers');
  }

  if (timeWindow.start >= timeWindow.end) {
    throw validationError('timeWindow.start must be before timeWindow.end');
  }
}

// Section: Lookup helpers
function findDevice(deviceId) {
  return state.deviceSpecs.find((d) => d.payload.deviceId === deviceId);
}

// Section: Consent creation with policy checks
function createConsent(input) {
  const timeWindow = input?.timeWindow;
  const deviceId = input?.deviceId || input?.deviceID;
  const maxFlexKW = input?.maxFlexKW;

  if (!deviceId) {
    throw validationError('deviceId is required');
  }

  validateTimeWindow(timeWindow);

  if (typeof maxFlexKW !== 'number' || maxFlexKW <= 0) {
    throw validationErrorWithStatus('invalid_maxFlexKW', 422);
  }

  const device = findDevice(deviceId);
  if (!device) {
    throw validationErrorWithStatus('unknown_device', 404, { code: 'unknown_device' });
  }

  // Whitelist check (device-level)
  if (!deviceWhitelist.has(deviceId)) {
    throw validationErrorWithStatus('device_not_trusted', 403, {
      code: 'device_not_trusted',
      deviceId,
      message: 'Device is not on whitelist',
    });
  }

  // Prequal check
  selectPrequalification(state.prequals, deviceId, timeWindow);

  const available = device.payload.availableFlexKW;
  if (typeof available !== 'number') {
    throw validationError('availableFlexKW missing on device');
  }

  if (available <= 0) {
    const err = validationErrorWithStatus('no_flex_available', 409, { code: 'no_flex_available' });
    err.extra = { deviceId, availableFlexKW: available };
    throw err;
  }

  if (maxFlexKW > available) {
    const err = validationErrorWithStatus('maxFlex_exceeds_available', 409, { code: 'maxFlex_exceeds_available' });
    err.extra = { deviceId, availableFlexKW: available };
    throw err;
  }

  const cap = device.payload.maxFlexCapKW;
  if (typeof cap === 'number' && maxFlexKW > cap) {
    const err = validationErrorWithStatus('maxFlex_exceeds_device_cap', 422, {
      code: 'maxFlex_exceeds_device_cap',
      deviceCap: cap,
      deviceId,
    });
    throw err;
  }

  const consent = {
    type: 'ConsentCredential',
    id: uuidv4(),
    issuer: agentId,
    holder: input?.holder || defaultAggregatorUrl,
    timestamp: Date.now(),
    payload: {
      deviceId,
      oemId: device.payload.oemId,
      timeWindow,
      maxFlexKW,
      consentGivenToAggregator: true,
    },
  };

  state.lastConsent = consent;
  return { consent, device };
}

// Section: Post-push accounting (consume available flex)
function decrementFlex(deviceId, amount) {
  const device = findDevice(deviceId);
  if (!device) return;
  device.payload.availableFlexKW = device.payload.availableFlexKW - amount;
}

// Section: Consent issuance endpoint
app.post('/issue/consent', (req, res) => {
  try {
    const { consent } = createConsent(req.body || {});
    res.status(201).json(consent);
  } catch (err) {
    res.status(err.statusCode || 400).json({ error: err.message, ...(err.extra || {}) });
  }
});

// Section: Consent dispatch to Aggregator
app.post('/push/consent-to-aggregator', async (req, res) => {
  const body = req.body || {};
  const targetBase = (body.aggregatorUrl || defaultAggregatorUrl || '').replace(/\/$/, '');

  try {
    const { consent } = createConsent(body);

    const response = await fetch(`${targetBase}/ingest/consent`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(consent),
    });

    const responseText = await response.text();
    let parsedResponse = null;
    try {
      parsedResponse = JSON.parse(responseText);
    } catch (_) {
      parsedResponse = responseText || null;
    }

    if (!response.ok) {
      throw new Error(`Aggregator responded with ${response.status}`);
    }

    decrementFlex(consent.payload.deviceId, consent.payload.maxFlexKW);

    res.json({
      status: 'sent',
      target: targetBase || defaultAggregatorUrl,
      aggregatorResponse: parsedResponse,
    });
  } catch (err) {
    const status = err.statusCode || 502;
    const payload =
      status === 400 || status === 403 || status === 404 || status === 409 || status === 422
        ? { error: err.message, ...(err.extra || {}) }
        : { error: 'Failed to push consent', reason: err.message };
    res.status(status).json(payload);
  }
});

// Section: Seed demo data for OEM + devices (prequals issued by TSO)
app.post('/admin/seed-one-oem-five-devices', (req, res) => {
  const { oem, devices } = buildSeedOneOemFiveDevices();
  // reset state
  state.deviceSpecs = [];
  state.prequals = [];
  state.oems = [];
  deviceWhitelist.clear();

  storeOem(oem);
  const storedDevices = devices.map((d) => storeDeviceSpec(d));

  // whitelist dev-1..dev-4
  ['dev-1', 'dev-2', 'dev-3', 'dev-4'].forEach((id) => deviceWhitelist.add(id));

  Promise.all(
    storedDevices.map((d) =>
      pushDeviceSpecToAggregator(d).catch((err) => ({
        ok: false,
        status: 502,
        body: { error: 'Aggregator unreachable', reason: err.message },
      }))
    )
  )
    .then((aggResponses) => {
      res.json({
        status: 'seeded',
        oems: 1,
        devices: devices.length,
        whitelist: Array.from(deviceWhitelist),
        aggregatorResponses: aggResponses,
      });
    })
    .catch((err) =>
      res.status(502).json({ error: 'Aggregator unreachable', reason: err.message })
    );
});

// Section: Server boot
const port = process.env.PORT || 8081;
if (require.main === module) {
  app.listen(port, () => {
    console.log(`Agent service listening on port ${port}`);
  });
}

// Section: Exports for tests
module.exports = {
  state,
  validateTimeWindow,
  createConsent,
  selectPrequalification,
  storePrequalification,
  storeDeviceSpec,
  pushDeviceSpecToAggregator,
  storeOem,
  findDevice,
  decrementFlex,
  deviceWhitelist,
};


