# Landing (React + Vite)

Landing de conversion que captura UTM, dispara eventos de funnel y conserva `eventId` para correlacionar navegacion y pago.

## Deploy

- https://s02-26-equipo-15-web-app-developmen.vercel.app/

## Variables de entorno

Crear `.env` desde `.env.example`:

```bash
cp .env.example .env
```

Variables clave:

```env
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PAYMENT_LINK=https://buy.stripe.com/...
VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX
VITE_META_PIXEL_ID=123456789012345
```

## Eventos que dispara

- `landing_view`
- `click_cta`
- `begin_checkout`
- `purchase` (confirmado server-side desde backend/webhook)

Notas:

- `eventId` se envia a backend por `POST /api/track`.
- En checkout se propaga a Stripe (`client_reference_id`) para correlacionar con webhook.

## Correr local

```bash
cd frontend/landing
npm install
npm run dev
```

- URL local: `http://localhost:5173`.

## Build

```bash
cd frontend/landing
npm run build
npm run preview
```
