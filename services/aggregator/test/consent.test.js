// Summary (FinalFinal): Added code to *.
// Purpose: document changes and explain behavior.
// Section: Test framework + helper under test
const test = require('node:test');
const assert = require('node:assert');
const { normalizeConsent } = require('../src/server');

// Section: ensure timeWindow is preserved in stored payloads
test('normalizeConsent preserves timeWindow', () => {
  const stored = normalizeConsent({
    type: 'ConsentCredential',
    payload: { timeWindow: { start: 1, end: 2 }, maxFlexKW: 5 },
  });
  assert.deepStrictEqual(stored.payload.timeWindow, { start: 1, end: 2 });
});


