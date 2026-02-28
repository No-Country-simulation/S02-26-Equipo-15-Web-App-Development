# Landing (React + Vite)

Landing de conversion que captura UTM, dispara eventos de funnel y conserva `eventId` para correlacionar navegacion y pago.

## Deploy

- https://s02-26-equipo-15-web-app-developmen.vercel.app/?utm_source=google&utm_medium=cpc&utm_campaign=demo_q1&utm_term=tax&utm_content=ad_a&gclid=g4&fbclid=meta

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
VITE_ATTRIBUTION_DEFAULT_SOURCE=(direct)
VITE_ATTRIBUTION_DEFAULT_MEDIUM=(none)
VITE_ATTRIBUTION_DEFAULT_CAMPAIGN=(not set)
VITE_ATTRIBUTION_REFERRAL_MEDIUM=referral
VITE_ATTRIBUTION_REFERRAL_CAMPAIGN=(referral)
```

## Eventos que dispara

- `landing_view`
- `click_cta`
- `begin_checkout`
- `purchase` (confirmado server-side desde backend/webhook)

Notas:

- `eventId` se envia a backend por `POST /api/track`.
- En checkout se propaga a Stripe (`client_reference_id`) para correlacionar con webhook.
- Si no llegan UTMs en la URL, la landing envia fallback en `POST /api/track`.
- Los valores de fallback se configuran por `.env`:
  - `VITE_ATTRIBUTION_DEFAULT_SOURCE`
  - `VITE_ATTRIBUTION_DEFAULT_MEDIUM`
  - `VITE_ATTRIBUTION_DEFAULT_CAMPAIGN`
  - `VITE_ATTRIBUTION_REFERRAL_MEDIUM`
  - `VITE_ATTRIBUTION_REFERRAL_CAMPAIGN`
- Si existe referrer externo, clasifica `utm_source=<hostname_referrer>` y usa los valores referral del `.env`.

## Pruebas de pago (Stripe test mode)

Casos utiles para validar estados en admin (`orders.business_status`):
- `SUCCESS`(pago aceptado): tarjeta `4242 4242 4242 4242`
- `FAILED` (pago rechazado): tarjeta `4000 0000 0000 0002`
- `FAILED` (fondos insuficientes): tarjeta `4000 0000 0000 9995`
- `FAILED`/intermedio (requiere accion 3DS): tarjeta `4000 0000 0000 3220`
- `PENDING` asincrono (Cuenta bancaria de EE.UU./ACH): seleccionar metodo bancario de prueba en checkout.

Notas:

- Verificar que el backend reciba webhooks de Stripe para que el estado cambie en `orders`.
- El estado final depende del evento webhook procesado (por eso un pago puede quedar `PENDING` temporalmente).

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
