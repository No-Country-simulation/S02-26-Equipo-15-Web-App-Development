# Resumen End-to-End (Actualizado)

## 1) Objetivo

Unificar adquisicion, tracking y conversion de pago con correlacion por `eventId`, persistencia en PostgreSQL e integraciones server-side auditables.

## 2) Flujo principal implementado

1. Landing genera `eventId` nuevo por carga y envia `POST /api/track`.
2. Backend:
   - acepta `eventId` opcional,
   - genera UUID si no viene,
   - upsert `tracking_session` (first-touch),
   - insert `tracking_event` idempotente,
   - responde `{ "eventId": "<uuid>" }`.
3. Landing redirige a Stripe con `client_reference_id=<eventId>`.
4. Stripe envia webhooks (`payment_intent.succeeded`, `checkout.session.completed`).
5. Backend valida firma, procesa idempotente y persiste:
   - `stripe_webhook_event` (ahora con `event_id`)
   - `orders` (sin duplicar)
   - `tracking_event` `purchase`
6. Backend dispara integraciones (segun flags):
   - `META_CAPI`
   - `GA4_MP`
   - `PIPEDRIVE`
7. Resultado por integracion queda en `integrations_log`.

## 3) Endpoints activos

- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

Nota: los endpoints admin ya existen en backend, pero el frontend `frontend/admin/` sigue pendiente de desarrollo.

## 4) Flags y config

- `TRACKING_ENABLED`
- `META_CAPI_ENABLED`
- `GA4_MP_ENABLED`
- `PIPEDRIVE_ENABLED`
- `STRIPE_WEBHOOK_SECRET`
- `META_PIXEL_ID`, `META_ACCESS_TOKEN`
- `GA4_MEASUREMENT_ID`, `GA4_API_SECRET`
- `PIPEDRIVE_API_TOKEN`
- `CORS_ALLOWED_ORIGINS`

## 5) Donde validar por tramo

- API track: `tracking_session`, `tracking_event`
- Stripe webhook: `stripe_webhook_event`
- Orden de pago: `orders`
- Integraciones server-side: `integrations_log`

Interpretacion de `integrations_log.status`:

- `SENT`: envio aceptado
- `SENT_WITH_WARNINGS`: aceptado con warnings (GA4 debug)
- `FAILED`: error de envio
- `SKIPPED`: deshabilitado o falta config

## 6) Notas clave de trazabilidad

- Es normal ver 2 filas en `stripe_webhook_event` para un mismo `event_id`:
  son 2 eventos Stripe distintos del mismo pago.
- Es normal ver `PAID` y `SUCCEEDED` en `orders.status`.
  El estado canonico para negocio es `orders.business_status`.
- En Meta CAPI, `response_payload` ya guarda respuesta (ej. `events_received`, `fbtrace_id`).
- En GA4 MP, `transaction_id` corresponde a `stripe_session_id`.

## 7) Auditoria en una sola consulta

```sql
WITH target AS (
  SELECT 'TU_EVENT_ID'::uuid AS event_id
)
SELECT
  'tracking_session' AS section,
  ts.event_id::text AS ref_1,
  NULL::text AS ref_2,
  NULL::text AS status,
  ts.created_at AS ts,
  NULL::text AS detail
FROM tracking_session ts
JOIN target t ON ts.event_id = t.event_id

UNION ALL

SELECT
  'tracking_event' AS section,
  te.event_id::text AS ref_1,
  te.event_type AS ref_2,
  COALESCE(te.currency, '') AS status,
  te.created_at AS ts,
  COALESCE(te.value::text, '') AS detail
FROM tracking_event te
JOIN target t ON te.event_id = t.event_id

UNION ALL

SELECT
  'orders' AS section,
  o.event_id::text AS ref_1,
  COALESCE(o.payment_intent_id, o.stripe_session_id) AS ref_2,
  COALESCE(o.business_status, o.status) AS status,
  o.created_at AS ts,
  'stripe_session=' || COALESCE(o.stripe_session_id, '') AS detail
FROM orders o
JOIN target t ON o.event_id = t.event_id

UNION ALL

SELECT
  'ga4_mp' AS section,
  il.reference_id AS ref_1,
  COALESCE(il.http_status::text, '') AS ref_2,
  il.status AS status,
  il.created_at AS ts,
  COALESCE(il.error_message, '') AS detail
FROM integrations_log il
JOIN target t ON il.reference_id = t.event_id::text
WHERE il.integration = 'GA4_MP'

UNION ALL

SELECT
  'meta_capi' AS section,
  il.reference_id AS ref_1,
  COALESCE(il.http_status::text, '') AS ref_2,
  il.status AS status,
  il.created_at AS ts,
  COALESCE(il.error_message, '') AS detail
FROM integrations_log il
JOIN target t ON il.reference_id = t.event_id::text
WHERE il.integration = 'META_CAPI'

UNION ALL

SELECT
  'stripe_webhook_event' AS section,
  swe.event_id::text AS ref_1,
  swe.stripe_event_id AS ref_2,
  swe.status AS status,
  swe.received_at AS ts,
  COALESCE(swe.error, '') AS detail
FROM stripe_webhook_event swe
JOIN target t ON swe.event_id = t.event_id

ORDER BY ts DESC;
```
