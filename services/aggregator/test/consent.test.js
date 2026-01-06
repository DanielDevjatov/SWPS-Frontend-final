const test = require('node:test');
const assert = require('node:assert');
const { normalizeConsent } = require('../src/server');

test('normalizeConsent preserves timeWindow', () => {
  const stored = normalizeConsent({
    type: 'ConsentCredential',
    payload: { timeWindow: { start: 1, end: 2 }, maxFlexKW: 5 },
  });
  assert.deepStrictEqual(stored.payload.timeWindow, { start: 1, end: 2 });
});
