# Resumen End-to-End (Actualizado)

## 1. Objetivo
Conectar adquisicion (ads), tracking y conversion de pago en un flujo unico con correlacion por `eventId`, persistencia en PostgreSQL e integraciones server-side.

## 2. Flujo principal implementado
1. La Landing envia `POST /api/track` con `eventType`, UTMs, `gclid/fbclid`, `landing_path` y `eventId` opcional.
2. Backend:
   - genera `eventId` si no viene,
   - hace upsert de `tracking_session` (first-touch, sin sobreescribir UTMs iniciales),
   - inserta `tracking_event` idempotente,
   - devuelve exactamente `{ "eventId": "<uuid>" }`.
3. Usuario paga en Stripe Checkout.
4. Stripe envia webhook a `POST /api/stripe/webhook`.
5. Backend valida firma y procesa idempotente:
   - registra estado en `stripe_webhook_event`,
   - crea/actualiza `orders` sin duplicar por `stripe_session_id`,
   - registra `purchase` en `tracking_event` cuando corresponde.
6. Backend dispara integraciones server-side (segun flags):
   - Meta CAPI,
   - GA4 Measurement Protocol,
   - Pipedrive (opcional).
7. Resultado de cada integracion queda en `integrations_log`.

## 3. Endpoints activos
- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

## 4. Flags y configuracion
- `TRACKING_ENABLED`
- `META_CAPI_ENABLED`
- `GA4_MP_ENABLED`
- `PIPEDRIVE_ENABLED`
- `STRIPE_WEBHOOK_SECRET`
- `META_PIXEL_ID`, `META_ACCESS_TOKEN`
- `GA4_MEASUREMENT_ID`, `GA4_API_SECRET`
- `PIPEDRIVE_API_TOKEN`
- `CORS_ALLOWED_ORIGINS`

## 5. Donde validar exito/falla por tramo
- **API Track**: tabla `tracking_session` y `tracking_event`.
- **Webhook Stripe**: tabla `stripe_webhook_event`.
  - `PROCESSED`: webhook valido y procesado.
  - `FAILED`: revisar columna `error` (ej. `Invalid Stripe signature`).
- **Registro de pago**: tabla `orders`.
- **Meta/GA4/Pipedrive server-side**: tabla `integrations_log`.
  - `SENT`: envio exitoso.
  - `FAILED`: error y status HTTP cuando exista.
  - `SKIPPED`: integracion deshabilitada o sin credenciales.

## 6. Nota importante sobre GA4(client) y Meta Pixel(client)
El backend **no puede confirmar** directamente los eventos client-side (`GA4 client` y `Meta Pixel client`) porque salen del navegador. Esos se validan en:
- GA4 DebugView / Reportes,
- Meta Events Manager.

La parte server-side si queda trazada en `integrations_log`.

## 7. Limpieza de esquema aplicada
- Se agrego migracion Flyway `V4__drop_legacy_tables.sql`.
- Se removieron tablas legadas: `users`, `attributions`, `landing_events`, `payments`.
- Desde esta migracion, el backend opera con un unico modelo activo:
  - `tracking_session`
  - `tracking_event`
  - `orders`
  - `stripe_webhook_event`
  - `integrations_log`
