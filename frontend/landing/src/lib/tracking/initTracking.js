const GA_MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID;
const META_PIXEL_ID = import.meta.env.VITE_META_PIXEL_ID;

let gaInitialized = false;
let metaInitialized = false;

const hasScript = (srcContains) =>
  !!document.querySelector(`script[src*="${srcContains.replace(/"/g, '')}"]`);

function initGA() {
  if (!GA_MEASUREMENT_ID || gaInitialized) return;

  if (!hasScript('googletagmanager.com/gtag/js')) {
    const gaScript = document.createElement('script');
    gaScript.async = true;
    gaScript.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(GA_MEASUREMENT_ID)}`;
    gaScript.setAttribute('data-ga4', GA_MEASUREMENT_ID);
    document.head.appendChild(gaScript);
  }

  window.dataLayer = window.dataLayer || [];
  window.gtag =
    window.gtag ||
    function gtag() {
      window.dataLayer.push(arguments);
    };

  window.gtag('js', new Date());
  window.gtag('config', GA_MEASUREMENT_ID, {
    anonymize_ip: true,
    send_page_view: false,
  });

  gaInitialized = true;
}

function initMetaPixel() {
  if (!META_PIXEL_ID || metaInitialized) return;

  if (!window.fbq) {
    const fbq = function fbq() {
      fbq.callMethod ? fbq.callMethod.apply(fbq, arguments) : fbq.queue.push(arguments);
    };
    fbq.push = fbq;
    fbq.loaded = true;
    fbq.version = '2.0';
    fbq.queue = [];
    window.fbq = fbq;

    if (!hasScript('connect.facebook.net')) {
      const pixelScript = document.createElement('script');
      pixelScript.async = true;
      pixelScript.src = 'https://connect.facebook.net/en_US/fbevents.js';
      pixelScript.setAttribute('data-meta-pixel', META_PIXEL_ID);
      document.head.appendChild(pixelScript);
    }
  }

  window.fbq('init', META_PIXEL_ID);

  metaInitialized = true;
}

export function initTracking() {
  if (typeof window === 'undefined') return;
  initGA();
  initMetaPixel();
}

export { GA_MEASUREMENT_ID, META_PIXEL_ID };
