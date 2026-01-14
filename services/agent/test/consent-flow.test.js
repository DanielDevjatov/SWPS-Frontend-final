// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Test framework + server under test
const test = require('node:test');
const assert = require('node:assert');
const server = require('../src/server');

// Section: Reset shared in-memory state between tests
function reset() {
  server.state.deviceSpecs = [];
  server.state.prequals = [];
  server.state.oems = [];
  server.state.lastConsent = null;
  server.deviceWhitelist.clear();
}

// Section: Seed helper for OEM credentials
function seedOem(oemId = 'oem-test') {
  server.storeOem({
    type: 'OEMCredential',
    payload: { oemId, name: oemId, status: 'active' },
  });
  return oemId;
}

// Section: Seed helper for device specs + whitelist inclusion
function seedDevice({ deviceId, availableFlexKW, oemId, maxFlexCapKW = 5 }) {
  if (!server.state.oems.find((o) => o.payload.oemId === oemId)) {
    seedOem(oemId);
  }
  server.storeDeviceSpec({
    type: 'DeviceSpecificationsCredential',
    payload: {
      deviceId,
      deviceName: deviceId,
      oemId,
      gridConnectionArea: 'area',
      ratedPowerKW: 10,
      availableFlexKW,
      maxFlexCapKW,
      minFlexCapKW: 0,
      deviceType: 'TestDevice',
      location: 'TestCity',
    },
  });
  server.deviceWhitelist.add(deviceId);
}

// Section: Seed helper for prequalification credentials
function seedPrequal({ deviceId, oemId, validFrom = 0, validTo = 1000 }) {
  server.storePrequalification({
    type: 'PrequalificationCredential',
    issuer: 'tso-1',
    payload: { prequalificationType: 'type', gridConnectionArea: 'area', validFrom, validTo, deviceId, oemId },
  });
}

// Section: Policy test cases for consent creation
test('push consent fails for unknown device', () => {
  reset();
  assert.throws(() => server.createConsent({ deviceId: 'unknown', timeWindow: { start: 1, end: 2 }, maxFlexKW: 1 }), /unknown_device/);
});

test('push consent fails when prequal missing', () => {
  reset();
  seedDevice({ deviceId: 'dev-prequal-missing', availableFlexKW: 5, oemId: 'oem-x' });
  assert.throws(() => server.createConsent({ deviceId: 'dev-prequal-missing', timeWindow: { start: 1, end: 2 }, maxFlexKW: 1 }), /no valid prequalification/);
});

test('push consent fails when no flex available', () => {
  reset();
  seedDevice({ deviceId: 'dev-no-flex', availableFlexKW: 0, oemId: 'oem-x' });
  seedPrequal({ deviceId: 'dev-no-flex', oemId: 'oem-x' });
  const err = assert.throws(() => server.createConsent({ deviceId: 'dev-no-flex', timeWindow: { start: 1, end: 2 }, maxFlexKW: 1 }));
  assert.match(err.message, /no_flex_available/);
});

test('push consent fails when maxFlexKW exceeds available', () => {
  reset();
  seedDevice({ deviceId: 'dev-too-much', availableFlexKW: 1, oemId: 'oem-x' });
  seedPrequal({ deviceId: 'dev-too-much', oemId: 'oem-x' });
  const err = assert.throws(() => server.createConsent({ deviceId: 'dev-too-much', timeWindow: { start: 1, end: 2 }, maxFlexKW: 2 }));
  assert.match(err.message, /maxFlex_exceeds_available/);
});

test('push consent success reduces availableFlexKW', () => {
  reset();
  seedDevice({ deviceId: 'dev-ok', availableFlexKW: 2, oemId: 'oem-ok' });
  seedPrequal({ deviceId: 'dev-ok', oemId: 'oem-ok' });
  const { consent } = server.createConsent({ deviceId: 'dev-ok', timeWindow: { start: 1, end: 2 }, maxFlexKW: 2 });
  server.decrementFlex(consent.payload.deviceId, consent.payload.maxFlexKW);
  const device = server.findDevice('dev-ok');
  assert.strictEqual(device.payload.availableFlexKW, 0);
});

test('push consent fails for non-whitelisted oem', () => {
  reset();
  // Device is not added to whitelist
  server.storeOem({ type: 'OEMCredential', payload: { oemId: 'blocked-oem', name: 'Blocked', status: 'active' } });
  server.storeDeviceSpec({
    type: 'DeviceSpecificationsCredential',
    payload: {
      deviceId: 'dev-block',
      deviceName: 'Block',
      oemId: 'blocked-oem',
      gridConnectionArea: 'area',
      ratedPowerKW: 10,
      availableFlexKW: 1,
      maxFlexCapKW: 1,
      minFlexCapKW: 0,
      deviceType: 'Test',
      location: 'TestCity',
    },
  });
  server.storePrequalification({
    type: 'PrequalificationCredential',
    payload: { prequalificationType: 'type', gridConnectionArea: 'area', validFrom: 0, validTo: 10, deviceId: 'dev-block', oemId: 'blocked-oem' },
  });
  const err = assert.throws(() => server.createConsent({ deviceId: 'dev-block', timeWindow: { start: 1, end: 2 }, maxFlexKW: 1 }));
  assert.match(err.message, /device_not_trusted/);
});

test('device listing per OEM', () => {
  reset();
  seedDevice({ deviceId: 'dev-1', availableFlexKW: 1, oemId: 'oem-1' });
  seedDevice({ deviceId: 'dev-2', availableFlexKW: 1, oemId: 'oem-1' });
  seedDevice({ deviceId: 'dev-3', availableFlexKW: 1, oemId: 'oem-1' });
  seedDevice({ deviceId: 'dev-4', availableFlexKW: 1, oemId: 'oem-2' });
  seedDevice({ deviceId: 'dev-5', availableFlexKW: 0, oemId: 'oem-2' });

  const oem1 = server.state.deviceSpecs.filter((d) => d.payload.oemId === 'oem-1');
  const oem2 = server.state.deviceSpecs.filter((d) => d.payload.oemId === 'oem-2');
  assert.strictEqual(oem1.length, 3);
  assert.strictEqual(oem2.length, 2);
});

test('maxFlex exceeds device cap fails', () => {
  reset();
  seedDevice({ deviceId: 'dev-cap', availableFlexKW: 10, oemId: 'oem-1', maxFlexCapKW: 1 });
  seedPrequal({ deviceId: 'dev-cap', oemId: 'oem-1' });
  const err = assert.throws(() => server.createConsent({ deviceId: 'dev-cap', timeWindow: { start: 1, end: 2 }, maxFlexKW: 2 }));
  assert.match(err.message, /maxFlex_exceeds_device_cap/);
});

test('prequal ingest rejects non-TSO issuer', () => {
  reset();
  const err = assert.throws(() =>
    server.storePrequalification({
      type: 'PrequalificationCredential',
      issuer: 'not-tso',
      payload: {
        prequalificationType: 'type',
        gridConnectionArea: 'area',
        validFrom: 0,
        validTo: 10,
        deviceId: 'dev-x',
        oemId: 'oem-x',
      },
    })
  );
  assert.match(err.message, /invalid_issuer/);
});


