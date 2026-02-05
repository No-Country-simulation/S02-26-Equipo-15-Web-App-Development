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
- Producción (Vercel): Project Settings → Environment Variables → añade las mismas claves para los entornos que uses.
- Local: copia `.env.example` a `.env` y completa los valores. Si no pones IDs de tracking, no se insertan scripts ni se envían eventos.

## Tracking implementado
- Inyección runtime de gtag.js y fbevents.js desde `src/lib/tracking/initTracking.js` (idempotente, solo si hay env vars).
- API de eventos en `src/lib/tracking/events.js`:
  - `trackPageView()` → GA4 `page_view`, Meta `PageView`.
  - `trackCTA()` → GA4 `select_item`, Meta `ClickCTA` (custom).
  - `trackBeginCheckout({ value, currency })` → GA4 `begin_checkout`, Meta `InitiateCheckout`.
- El CTA principal dispara `trackCTA` + `trackBeginCheckout` antes de redirigir al checkout.

## Cómo validar los eventos
- **GA4 DebugView**: abre la landing con el Tag Assistant en modo preview (o añade `?debug_mode=1`), revisa eventos `page_view`, `select_item`, `begin_checkout` en DebugView.
- **Meta Events Manager → Test Events**: ingresa el Pixel ID, abre la landing en la misma ventana y verifica `PageView`, `ClickCTA`, `InitiateCheckout`.

## Notas de QA
- Sin `VITE_GA_MEASUREMENT_ID` o `VITE_META_PIXEL_ID` no se cargan scripts ni se envían eventos (sin errores en consola).
- No se implementa `Purchase` ni tracking server-side aquí; el backend puede disparar conversiones server-side usando los mismos IDs.
- `.env`, `node_modules` y `dist` están ignorados por git.
