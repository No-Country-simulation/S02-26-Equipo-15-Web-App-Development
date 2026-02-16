const ATTRIBUTION_KEY = 'nc_attribution';
const EVENT_ID_KEY = 'nc_event_id';

const TRACKED_PARAMS = [
  'utm_source',
  'utm_medium',
  'utm_campaign',
  'utm_term',
  'utm_content',
  'gclid',
  'fbclid',
];

// Fallback when localStorage is unavailable (e.g., privacy mode).
const memoryStore = {};

function safeGet(key) {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage.getItem(key);
    }
  } catch {
    // ignore
  }
  return memoryStore[key] || null;
}

function safeSet(key, value) {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.setItem(key, value);
    }
  } catch {
    // ignore quota/security errors
  }
  memoryStore[key] = value;
}

function parseAttribution() {
  const raw = safeGet(ATTRIBUTION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function persistAttribution(data) {
  safeSet(ATTRIBUTION_KEY, JSON.stringify(data));
}

function generateEventId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
  const randomPart = Math.random().toString(36).slice(2, 10);
  return `evt-${Date.now().toString(36)}-${randomPart}`;
}

export function ensureEventId() {
  const existing = safeGet(EVENT_ID_KEY);
  if (existing) return existing;
  const next = generateEventId();
  safeSet(EVENT_ID_KEY, next);
  return next;
}

export function rotateEventId() {
  const next = generateEventId();
  safeSet(EVENT_ID_KEY, next);
  return next;
}

export function readAttribution() {
  if (typeof window === 'undefined') return null;

  const now = new Date().toISOString();
  const search = new URLSearchParams(window.location.search || '');
  const referrer = typeof document !== 'undefined' ? document.referrer || null : null;
  const landingPath = typeof window !== 'undefined' ? window.location.pathname : null;
  const existing = parseAttribution();

  const base = existing || { firstTouchAt: now };
  const next = { ...base, lastUpdatedAt: now };

  TRACKED_PARAMS.forEach((param) => {
    const value = search.get(param);
    if (value) {
      next[param] = value;
    } else if (existing && existing[param]) {
      next[param] = existing[param];
    }
  });

  if (!next.firstTouchAt) next.firstTouchAt = now;
  next.referrer = referrer ?? next.referrer ?? null;
  next.landing_path = landingPath ?? next.landing_path ?? null;

  persistAttribution(next);
  return next;
}

export function getStoredAttribution() {
  return parseAttribution();
}

export function getAttributionSnapshot() {
  return parseAttribution();
}
