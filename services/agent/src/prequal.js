function validationError(message, statusCode = 400) {
  const err = new Error(message);
  err.statusCode = statusCode;
  return err;
}

function normalizePrequalification(body, defaults = {}) {
  if (!body || body.type !== 'PrequalificationCredential') {
    throw validationError('Expected credential type PrequalificationCredential');
  }
  if (!body.payload || typeof body.payload !== 'object') {
    throw validationError('Missing credential payload');
  }

  const payload = { ...body.payload };
  payload.deviceId = payload.deviceId || payload.deviceID || defaults.deviceID;
  payload.oemId = payload.oemId || payload.oemID || defaults.oemId;

  if (typeof payload.validFrom !== 'number' || typeof payload.validTo !== 'number') {
    throw validationError('validFrom and validTo must be numbers');
  }

  if (payload.validFrom >= payload.validTo) {
    throw validationError('validFrom must be before validTo');
  }

  if (!payload.deviceId) {
    throw validationError('deviceId is required on prequalification');
  }

  return {
    type: 'PrequalificationCredential',
    id: body.id || defaults.id,
    issuer: body.issuer || defaults.issuer,
    holder: body.holder || defaults.holder,
    timestamp: body.timestamp || Date.now(),
    payload,
  };
}

function selectPrequalification(prequals, deviceId, timeWindow) {
  if (!prequals || !prequals.length) {
    throw validationError('no valid prequalification for device/timeWindow', 403);
  }
  const matches = prequals.filter((p) => {
    const payload = p.payload || {};
    const pid = payload.deviceId || payload.deviceID;
    const idOk = !pid || pid === deviceId;
    const hasWindow =
      typeof payload.validFrom === 'number' &&
      typeof payload.validTo === 'number' &&
      payload.validFrom <= timeWindow.start &&
      payload.validTo >= timeWindow.end;
    return idOk && hasWindow;
  });

  if (!matches.length) {
    throw validationError('no valid prequalification for device/timeWindow', 403);
  }

  return matches.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))[0];
}

module.exports = { normalizePrequalification, selectPrequalification, validationError };
