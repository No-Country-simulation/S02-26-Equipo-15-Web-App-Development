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

const DEFAULT_UTM_SOURCE = (import.meta.env.VITE_ATTRIBUTION_DEFAULT_SOURCE || '(direct)').trim();
const DEFAULT_UTM_MEDIUM = (import.meta.env.VITE_ATTRIBUTION_DEFAULT_MEDIUM || '(none)').trim();
const DEFAULT_UTM_CAMPAIGN = (import.meta.env.VITE_ATTRIBUTION_DEFAULT_CAMPAIGN || '(not set)').trim();
const REFERRAL_UTM_MEDIUM = (import.meta.env.VITE_ATTRIBUTION_REFERRAL_MEDIUM || 'referral').trim();
const REFERRAL_UTM_CAMPAIGN = (import.meta.env.VITE_ATTRIBUTION_REFERRAL_CAMPAIGN || '(referral)').trim();

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

function normalize(value) {
  if (value == null) return null;
  const trimmed = String(value).trim();
  return trimmed.length > 0 ? trimmed : null;
}

function resolveReferrerSource(referrer) {
  const safeReferrer = normalize(referrer);
  if (!safeReferrer || typeof window === 'undefined') return null;

  try {
    const referrerUrl = new URL(safeReferrer);
    const currentHost = window.location.hostname;
    if (!referrerUrl.hostname || referrerUrl.hostname === currentHost) {
      return null;
    }
    return referrerUrl.hostname;
  } catch {
    return null;
  }
}

// Normalized attribution payload used in POST /api/track.
// If there are no UTM params, classify as direct traffic to avoid NULL campaigns.
export function buildTrackPayloadAttribution(attribution = {}) {
  const utmSource = normalize(attribution?.utm_source);
  const utmMedium = normalize(attribution?.utm_medium);
  const utmCampaign = normalize(attribution?.utm_campaign);
  const utmTerm = normalize(attribution?.utm_term);
  const utmContent = normalize(attribution?.utm_content);
  const gclid = normalize(attribution?.gclid);
  const fbclid = normalize(attribution?.fbclid);

  const referrerSource = resolveReferrerSource(attribution?.referrer);
  const hasCampaignSignals = Boolean(utmSource || utmMedium || utmCampaign || gclid || fbclid);

  if (hasCampaignSignals) {
    return {
      utm_source: utmSource || DEFAULT_UTM_SOURCE,
      utm_medium: utmMedium || DEFAULT_UTM_MEDIUM,
      utm_campaign: utmCampaign || DEFAULT_UTM_CAMPAIGN,
      utm_term: utmTerm,
      utm_content: utmContent,
      gclid,
      fbclid,
    };
  }

  if (referrerSource) {
    return {
      utm_source: referrerSource,
      utm_medium: REFERRAL_UTM_MEDIUM,
      utm_campaign: REFERRAL_UTM_CAMPAIGN,
      utm_term: utmTerm,
      utm_content: utmContent,
      gclid,
      fbclid,
    };
  }

  return {
    utm_source: DEFAULT_UTM_SOURCE,
    utm_medium: DEFAULT_UTM_MEDIUM,
    utm_campaign: DEFAULT_UTM_CAMPAIGN,
    utm_term: utmTerm,
    utm_content: utmContent,
    gclid,
    fbclid,
  };
}
