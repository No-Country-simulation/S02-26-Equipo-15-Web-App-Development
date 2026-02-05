import { GA_MEASUREMENT_ID, META_PIXEL_ID, initTracking } from './initTracking';

const canSendGA = () => typeof window !== 'undefined' && typeof window.gtag === 'function' && !!GA_MEASUREMENT_ID;
const canSendMeta = () => typeof window !== 'undefined' && typeof window.fbq === 'function' && !!META_PIXEL_ID;

export function trackPageView(path) {
  initTracking();

  const page_path = path || (typeof window !== 'undefined' ? window.location.pathname : undefined);
  const page_location = typeof window !== 'undefined' ? window.location.href : undefined;

  if (canSendGA()) {
    window.gtag('event', 'page_view', {
      send_to: GA_MEASUREMENT_ID,
      page_location,
      page_path,
    });
  }

  if (canSendMeta()) {
    window.fbq('track', 'PageView');
  }
}

export function trackCTA({ eventId, label = 'CTA principal' } = {}) {
  initTracking();

  if (canSendGA()) {
    window.gtag('event', 'click_cta', {
      send_to: GA_MEASUREMENT_ID,
      item_list_name: 'cta',
      item_name: label,
      event_label: label,
      ...(eventId ? { event_id: eventId } : {}),
    });
  }

  if (canSendMeta()) {
    const payload = { ...(eventId ? { eventID: eventId } : {}), label };
    window.fbq('trackCustom', 'ClickCTA', payload);
  }
}

export function trackBeginCheckout({ value, currency = 'USD', eventId } = {}) {
  initTracking();

  if (canSendGA()) {
    const payload = {
      send_to: GA_MEASUREMENT_ID,
      currency,
      ...(typeof value === 'number' ? { value } : {}),
      ...(eventId ? { event_id: eventId } : {}),
    };

    window.gtag('event', 'begin_checkout', payload);
  }

  if (canSendMeta()) {
    const payload = {
      ...(typeof value === 'number' ? { value } : {}),
      ...(currency ? { currency } : {}),
      ...(eventId ? { eventID: eventId } : {}),
    };
    window.fbq('track', 'InitiateCheckout', payload);
  }
}
