# Landing (React + Vite)

Landing de conversion con tracking client-side y redireccion a Stripe Checkout.

## Requisitos

- Node 18+
- npm 9+

## Ejecutar local

```bash
cd frontend/landing
npm install
npm run dev
```

App local: `http://localhost:5173`

## Variables de entorno

Crear `.env` desde `.env.example`:

```bash
cp .env.example .env
```

Variables usadas:

```env
VITE_STRIPE_PAYMENT_LINK=https://buy.stripe.com/...
VITE_API_URL=http://localhost:8080
# Compat: tambien acepta VITE_API_BASE
VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX
VITE_META_PIXEL_ID=123456789012345
```

En Vercel, configurar estas variables por entorno y redeployar.

## Comportamiento de tracking

- En cada carga de la landing se genera un `eventId` nuevo.
- Se envia `landing_view` a backend en `POST /api/track` con ese `eventId`.
- El backend puede devolver un `eventId` y el frontend lo conserva para los eventos siguientes de esa visita.
- Al hacer click en CTA:
  - envia `click_cta` y `begin_checkout` (GA4 + Meta Pixel)
  - redirige a Stripe con:
    - `client_reference_id=<eventId>`
    - `nc_event_id=<eventId>` (compat legacy)
    - UTM/gclid/fbclid

## Eventos client-side

- `trackPageView(path?)` -> GA4 `page_view`, Meta `PageView`
- `trackCTA({ eventId, label })` -> GA4 `click_cta`, Meta `ClickCTA`
- `trackBeginCheckout({ value, currency, eventId })` -> GA4 `begin_checkout`, Meta `InitiateCheckout`

## Debug rapido

- DevTools > Network:
  - GA4: filtrar `collect?v=2`
  - Meta: filtrar `tr?id=`
- Console:
  - validar `import.meta.env.VITE_GA_MEASUREMENT_ID`
  - validar `import.meta.env.VITE_META_PIXEL_ID`

Si cambias `.env`, reinicia `npm run dev`.
