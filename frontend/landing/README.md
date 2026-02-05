# Landing (React + Vite)

Landing con tracking client-side listo para GA4 y Meta Pixel.

## Requisitos
- Node 18+
- npm 9+

## Instalación y ejecución local
```bash
cd frontend/landing
npm install
npm run dev
```
La app queda en http://localhost:5173.

## Variables de entorno
Configura un `.env` (no se versiona) tomando como referencia `.env.example`:
```
VITE_STRIPE_PAYMENT_LINK=
VITE_API_BASE=
VITE_GA_MEASUREMENT_ID=
VITE_META_PIXEL_ID=
```
- Producción (Vercel): Project Settings → Environment Variables → añade las mismas claves para Production/Preview.
- Local: copia `.env.example` a `.env` y completa los valores. Si no pones IDs de tracking, no se insertan scripts ni se envían eventos.

## Tracking implementado
- Inyección runtime de gtag.js y fbevents.js desde `src/lib/tracking/initTracking.js` (idempotente, solo si hay env vars).
- API de eventos en `src/lib/tracking/events.js`:
  - `trackPageView(path?)` → GA4 `page_view`, Meta `PageView`.
  - `trackCTA({ eventId?, label? })` → GA4 `click_cta`, Meta `ClickCTA` (custom) con `event_id`/`eventID` para deduplicación futura con CAPI.
  - `trackBeginCheckout({ value?, currency?, eventId? })` → GA4 `begin_checkout`, Meta `InitiateCheckout` con `event_id`/`eventID`.
- El CTA principal dispara `trackCTA` + `trackBeginCheckout` antes de redirigir al checkout (valor por defecto USD 0).

## Cómo validar los eventos
- **GA4**: en DebugView (o Realtime) revisa `page_view`, `click_cta`, `begin_checkout`. Usa Tag Assistant preview o `?debug_mode=1`.
- **Meta Events Manager → Test Events**: ingresa el Pixel ID, abre la landing en la misma ventana y verifica `PageView`, `ClickCTA`, `InitiateCheckout` (con eventID cuando se envía).

## Notas de QA
- Sin `VITE_GA_MEASUREMENT_ID` o `VITE_META_PIXEL_ID` no se cargan scripts ni se envían eventos (sin errores en consola).
- No se implementa `Purchase` ni tracking server-side aquí; se hará por webhook/CAPI en el backend.
- `.env`, `node_modules` y `dist` están ignorados por git.
