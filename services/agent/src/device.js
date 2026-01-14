// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Imports used for UUIDs and shared validation errors
const { v4: uuidv4 } = require('uuid');
const { validationError } = require('./prequal');

// Section: Normalize + validate device specification payloads
function normalizeDeviceSpec(body, defaults = {}) {
  if (!body || body.type !== 'DeviceSpecificationsCredential') {
    throw validationError('Expected credential type DeviceSpecificationsCredential');
  }
  if (!body.payload || typeof body.payload !== 'object') {
    throw validationError('Missing credential payload');
  }
  const payload = { ...body.payload };
  payload.deviceId = payload.deviceId || payload.deviceID;

  if (!payload.deviceId) {
    throw validationError('deviceId is required');
  }
  if (!payload.oemId) {
    throw validationError('oemId is required');
  }
  if (!payload.gridConnectionArea) {
    throw validationError('gridConnectionArea is required');
  }
  if (typeof payload.ratedPowerKW !== 'number') {
    throw validationError('ratedPowerKW must be a number');
  }
  if (typeof payload.availableFlexKW !== 'number') {
    throw validationError('availableFlexKW must be a number');
  }
  if (typeof payload.maxFlexCapKW !== 'number') {
    throw validationError('maxFlexCapKW must be a number');
  }
  if (payload.minFlexCapKW !== undefined && typeof payload.minFlexCapKW !== 'number') {
    throw validationError('minFlexCapKW must be a number if provided');
  }
  if (!payload.deviceType) {
    throw validationError('deviceType is required');
  }
  if (!payload.location) {
    throw validationError('location is required');
  }

  // Build the normalized credential with standard metadata fields.
  return {
    type: 'DeviceSpecificationsCredential',
    id: body.id || defaults.id || uuidv4(),
    issuer: body.issuer || defaults.issuer,
    holder: body.holder || defaults.holder,
    timestamp: body.timestamp || Date.now(),
    payload,
  };
}

// Section: Public module API
module.exports = { normalizeDeviceSpec };


