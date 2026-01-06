const test = require('node:test');
const assert = require('node:assert');
const { verifyAggregatorPresentation } = require('../src/server');

test('verifyAggregatorPresentation rejects invalid totals', async () => {
  const result = await verifyAggregatorPresentation(
    {
      type: 'AggregatorPresentation',
      timeWindow: { start: 1, end: 2 },
      totalFlexKW: -1,
      consentCount: 1,
      proof: {},
      publicSignals: [0],
    },
    { skipZkp: true }
  );
  assert.strictEqual(result.status, 'invalid');
});

test('verifyAggregatorPresentation accepts matching publicSignals when skipZkp', async () => {
  const result = await verifyAggregatorPresentation(
    {
      type: 'AggregatorPresentation',
      timeWindow: { start: 1, end: 2 },
      totalFlexKW: 5,
      consentCount: 1,
      proof: { dummy: true },
      publicSignals: [5],
    },
    { skipZkp: true }
  );
  assert.strictEqual(result.status, 'valid');
});
