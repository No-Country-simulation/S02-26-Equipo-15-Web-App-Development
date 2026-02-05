const DEFAULT_PAYMENT_LINK = 'https://buy.stripe.com/test_dRm6oH5bY54Wc84e1p4ow00';
const TRACKED_PARAMS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'gclid', 'fbclid'];

function resolvePaymentLink() {
  return import.meta.env.VITE_STRIPE_PAYMENT_LINK || DEFAULT_PAYMENT_LINK;
}

export function buildStripeUrl(attribution = {}, eventId) {
  const paymentLink = resolvePaymentLink();
  const url = new URL(paymentLink);

  if (eventId) url.searchParams.set('nc_event_id', eventId);

  TRACKED_PARAMS.forEach((param) => {
    const value = attribution?.[param];
    if (value) url.searchParams.set(param, value);
  });

  return url.toString();
}

export function redirectToStripe(attribution = {}, eventId) {
  const targetUrl = buildStripeUrl(attribution, eventId);
  if (typeof window === 'undefined') {
    throw new Error('Stripe redirect must run in the browser');
  }
  window.location.href = targetUrl;
}
