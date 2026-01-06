const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const { normalizePrequalification, selectPrequalification, validationError } = require('./prequal');
const { normalizeDeviceSpec } = require('./device');
const { buildSeedOneOemFiveDevices } = require('./seed');

const app = express();
app.use(express.json());
app.use(morgan('tiny'));
app.use(cors());

const state = {
  deviceSpecs: [],
  prequals: [],
  oems: [],
  lastConsent: null,
};

// Device whitelist (deviceIds)
const initialDeviceWhitelist = (process.env.DEVICE_WHITELIST || '').split(',').map((s) => s.trim()).filter(Boolean);
const deviceWhitelist = new Set(initialDeviceWhitelist);

const agentId = process.env.AGENT_ID || 'agent-1';
const defaultAggregatorUrl = process.env.AGGREGATOR_URL || 'http://localhost:8082';

function validationErrorWithStatus(message, statusCode = 400, extra = {}) {
  const err = validationError(message);
  err.statusCode = statusCode;
  Object.assign(err, extra);
  if (Object.keys(extra || {}).length) {
    err.extra = extra;
  }
  return err;
}

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

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

function storeDeviceSpec(body) {
  const stored = normalizeDeviceSpec(body, { issuer: agentId, holder: agentId });
  state.deviceSpecs = state.deviceSpecs.filter((d) => d.payload.deviceId !== stored.payload.deviceId);
  state.deviceSpecs.push(stored);
  return stored;
}

function storePrequalification(body) {
  const stored = normalizePrequalification(body, {
    deviceID: body.payload?.deviceId || body.payload?.deviceID,
    oemId: body.payload?.oemId,
    issuer: agentId,
    holder: agentId,
    id: uuidv4(),
  });
  state.prequals.push(stored);
  return stored;
}

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
    res.status(201).json({ status: 'stored', id: stored.id });
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

function validateTimeWindow(timeWindow) {
  if (!timeWindow || typeof timeWindow.start !== 'number' || typeof timeWindow.end !== 'number') {
    throw validationError('timeWindow.start and timeWindow.end must be numbers');
  }

  if (timeWindow.start >= timeWindow.end) {
    throw validationError('timeWindow.start must be before timeWindow.end');
  }
}

function findDevice(deviceId) {
  return state.deviceSpecs.find((d) => d.payload.deviceId === deviceId);
}

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

function decrementFlex(deviceId, amount) {
  const device = findDevice(deviceId);
  if (!device) return;
  device.payload.availableFlexKW = device.payload.availableFlexKW - amount;
}

app.post('/issue/consent', (req, res) => {
  try {
    const { consent } = createConsent(req.body || {});
    res.status(201).json(consent);
  } catch (err) {
    res.status(err.statusCode || 400).json({ error: err.message, ...(err.extra || {}) });
  }
});

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

app.post('/admin/seed-one-oem-five-devices', (req, res) => {
  const { oem, devices } = buildSeedOneOemFiveDevices();
  // reset state
  state.deviceSpecs = [];
  state.prequals = [];
  state.oems = [];
  deviceWhitelist.clear();

  storeOem(oem);
  devices.forEach((d) => {
    storeDeviceSpec(d);
    // add a wide prequalification per device
    storePrequalification({
      type: 'PrequalificationCredential',
      payload: {
        prequalificationType: 'seed',
        gridConnectionArea: d.payload.gridConnectionArea,
        validFrom: 0,
        validTo: Date.now() + 1000 * 60 * 60 * 24 * 365, // ~1 Jahr
        deviceId: d.payload.deviceId,
        oemId: d.payload.oemId,
      },
    });
  });

  // whitelist dev-1..dev-4
  ['dev-1', 'dev-2', 'dev-3', 'dev-4'].forEach((id) => deviceWhitelist.add(id));

  res.json({ status: 'seeded', oems: 1, devices: devices.length, whitelist: Array.from(deviceWhitelist) });
});

const port = process.env.PORT || 8081;
if (require.main === module) {
  app.listen(port, () => {
    console.log(`Agent service listening on port ${port}`);
  });
}

module.exports = {
  state,
  validateTimeWindow,
  createConsent,
  selectPrequalification,
  storePrequalification,
  storeDeviceSpec,
  storeOem,
  findDevice,
  decrementFlex,
  deviceWhitelist,
};
