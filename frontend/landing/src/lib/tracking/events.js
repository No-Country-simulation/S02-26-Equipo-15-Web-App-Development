import { GA_MEASUREMENT_ID, META_PIXEL_ID, initTracking } from './initTracking';

const canSendGA = () => typeof window !== 'undefined' && typeof window.gtag === 'function' && !!GA_MEASUREMENT_ID;
const canSendMeta = () => typeof window !== 'undefined' && typeof window.fbq === 'function' && !!META_PIXEL_ID;

export function trackPageView() {
  initTracking();

  if (canSendGA()) {
    window.gtag('event', 'page_view', {
      send_to: GA_MEASUREMENT_ID,
      page_location: window.location.href,
      page_path: window.location.pathname,
    });
  }

  if (canSendMeta()) {
    window.fbq('track', 'PageView');
  }
}

export function trackCTA() {
  initTracking();

  if (canSendGA()) {
    window.gtag('event', 'select_item', {
      send_to: GA_MEASUREMENT_ID,
      items: [{ item_name: 'CTA principal' }],
      event_label: 'ClickCTA',
    });
  }

  if (canSendMeta()) {
    window.fbq('trackCustom', 'ClickCTA');
  }
}

export function trackBeginCheckout({ value, currency } = {}) {
  initTracking();

  if (canSendGA()) {
    const payload = {
      send_to: GA_MEASUREMENT_ID,
      currency: currency || 'USD',
    };
    if (typeof value === 'number') payload.value = value;

    window.gtag('event', 'begin_checkout', payload);
  }

  if (canSendMeta()) {
    const payload = {};
    if (typeof value === 'number') payload.value = value;
    if (currency) payload.currency = currency;
    window.fbq('track', 'InitiateCheckout', payload);
  }
}
