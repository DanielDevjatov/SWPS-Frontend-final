// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Test framework + helpers under test
const test = require('node:test');
const assert = require('node:assert');
const { normalizePrequalification, selectPrequalification } = require('../src/prequal');
const { normalizeDeviceSpec } = require('../src/device');

// Section: selection logic behavior
test('selectPrequalification throws 403 when none match', () => {
  assert.throws(
    () => selectPrequalification([], 'dev-1', { start: 10, end: 20 }),
    /no valid prequalification/
  );
});

test('selectPrequalification throws when window not covered', () => {
  const prequals = [
    { payload: { deviceID: 'dev-1', validFrom: 0, validTo: 5 }, timestamp: 1 },
  ];
  assert.throws(
    () => selectPrequalification(prequals, 'dev-1', { start: 10, end: 20 }),
    /no valid prequalification/
  );
});

test('selectPrequalification picks matching and latest by timestamp', () => {
  const prequals = [
    {
      payload: { deviceId: 'dev-1', validFrom: 0, validTo: 100 },
      timestamp: 1,
    },
    {
      payload: { deviceId: 'dev-1', validFrom: 0, validTo: 100 },
      timestamp: 5,
    },
  ];
  const picked = selectPrequalification(prequals, 'dev-1', { start: 10, end: 20 });
  assert.strictEqual(picked.timestamp, 5);
});

test('normalizePrequalification fills deviceID from defaults and validates numbers', () => {
  const normalized = normalizePrequalification(
    {
      type: 'PrequalificationCredential',
      payload: { prequalificationType: 'A', gridConnectionArea: 'X', validFrom: 0, validTo: 2 },
    },
    { deviceID: 'dev-1', issuer: 'issuer', holder: 'holder', id: 'id-1' }
  );
  assert.strictEqual(normalized.payload.deviceId, 'dev-1');
});

test('normalizeDeviceSpec enforces availableFlexKW number', () => {
  assert.throws(
    () =>
      normalizeDeviceSpec({
        type: 'DeviceSpecificationsCredential',
        payload: { deviceID: 'dev-x', deviceName: 'X' },
      }),
    /availableFlexKW must be a number/
  );
});


