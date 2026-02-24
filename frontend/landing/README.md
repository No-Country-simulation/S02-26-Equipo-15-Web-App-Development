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
