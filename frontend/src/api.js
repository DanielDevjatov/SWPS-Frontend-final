const agentBase = process.env.REACT_APP_API_BASE || "http://localhost:8081";
const aggBase = process.env.REACT_APP_AGG_BASE || "http://localhost:8082";
const tsoBase = process.env.REACT_APP_TSO_BASE || "http://localhost:8083";

async function request(base, path, options = {}) {
  const url = `${base}${path}`;
  const resp = await fetch(url, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const text = await resp.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!resp.ok) {
    const err = new Error(data?.error || data?.message || `Request failed: ${resp.status}`);
    err.status = resp.status;
    err.body = data;
    throw err;
  }
  return data;
}

export const api = {
  agent: {
    seedDemo: () => request(agentBase, "/admin/seed-one-oem-five-devices", { method: "POST" }),
    listOems: () => request(agentBase, "/wallet/oems"),
    listDevices: (oemId) => request(agentBase, `/wallet/devices${oemId ? `?oemId=${encodeURIComponent(oemId)}` : ""}`),
    getDevice: (deviceId) => request(agentBase, `/wallet/devices/${deviceId}`),
    listWhitelist: () => request(agentBase, "/admin/whitelist"),
    pushConsent: ({ deviceId, timeWindow, maxFlexKW }) =>
      request(agentBase, "/push/consent-to-aggregator", {
        method: "POST",
        body: JSON.stringify({ deviceId, timeWindow, maxFlexKW }),
      }),
  },
  aggregator: {
    listConsents: () => request(aggBase, "/consents"),
    listPresentations: () => request(aggBase, "/presentations"),
    listDeviceBundles: () => request(aggBase, "/devices/bundles"),
    aggregate: (timeWindow) =>
      request(aggBase, "/presentations/aggregate", {
        method: "POST",
        body: JSON.stringify(timeWindow ? { timeWindow } : {}),
      }),
  },
  tso: {
    listPresentations: () => request(tsoBase, "/presentations"),
    listVerifications: () => request(tsoBase, "/verifications"),
    verifyPresentation: (presentation) =>
      request(tsoBase, "/verify/aggregator-presentation", {
        method: "POST",
        body: JSON.stringify(presentation),
      }),
    issuePrequalification: (payload) =>
      request(tsoBase, "/prequalifications/issue", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
  },
};
