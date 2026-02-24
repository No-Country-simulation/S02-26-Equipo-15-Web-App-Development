# Resumen End-to-End (Actualizado)

## 0) Contexto de negocio

Se busca mejorar conversion de trafico pago (Google/Meta) hacia compra en Stripe para un servicio de incorporacion, impuestos y bookkeeping en EE.UU.

Valor esperado de este flujo:

- atribucion confiable de compra por canal/campana,
- visibilidad del funnel completo en admin,
- capacidad de optimizar inversion en ads en base a conversion real.

## 1) Objetivo

Unificar adquisicion, tracking y conversion de pago con correlacion por `eventId`, persistencia en PostgreSQL, medicion en Google/Meta y monitoreo operativo desde panel admin.

## 2) Flujo principal implementado

1. Landing genera `eventId` nuevo por carga y envia `POST /api/track`.
2. Backend:
   - acepta `eventId` opcional,
   - genera UUID si no viene,
   - upsert `tracking_session` (first-touch),
   - insert `tracking_event` idempotente,
   - responde `{ "eventId": "<uuid>" }`.
3. Landing dispara eventos client-side:
   - Google Analytics 4 (`page_view`, `click_cta`, `begin_checkout`)
   - Meta Pixel (`PageView`, `ClickCTA`, `InitiateCheckout`)
4. Landing redirige a Stripe con `client_reference_id=<eventId>`.
5. Stripe envia webhooks (`payment_intent.succeeded`, `checkout.session.completed`).
6. Backend valida firma, procesa idempotente y persiste:
   - `stripe_webhook_event` (ahora con `event_id`)
   - `orders` (sin duplicar)
   - `tracking_event` `purchase`
7. Backend dispara integraciones server-side (segun flags):
   - `GA4_MP` (Google Analytics 4 MP)
   - `META_CAPI`
8. Resultado por integracion queda en `integrations_log`.
9. Admin consulta `sessions/events/metrics` via `GET /api/admin/*`.

## 3) Endpoints activos

- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

Nota: los endpoints admin en backend y el frontend `frontend/admin/` estan implementados y operativos.

## 4) Flags y config

- `TRACKING_ENABLED`
- `META_CAPI_ENABLED`
- `GA4_MP_ENABLED`
- `STRIPE_WEBHOOK_SECRET`
- `META_PIXEL_ID`, `META_ACCESS_TOKEN`
- `GA4_MEASUREMENT_ID`, `GA4_API_SECRET`
- `CORS_ALLOWED_ORIGINS`

## 5) Donde validar por tramo

- API track: `tracking_session`, `tracking_event`
- Stripe webhook: `stripe_webhook_event`
- Orden de pago: `orders`
- Integraciones server-side: `integrations_log`

Interpretacion de `integrations_log.status`:

- `SENT`: envio aceptado.
- `SENT_WITH_WARNINGS`: aceptado con warnings (GA4 debug).
- `FAILED`: error de envio.
- `SKIPPED`: deshabilitado o falta config.

## 6) Notas clave de trazabilidad

- Es normal ver 2 filas en `stripe_webhook_event` para un mismo `event_id`:
  son 2 eventos Stripe distintos del mismo pago.
- Es normal ver `PAID` y `SUCCEEDED` en `orders.status`.
  El estado canonico para negocio es `orders.business_status`.
- En Meta CAPI, `response_payload` ya guarda respuesta (ej. `events_received`, `fbtrace_id`).
- En GA4 MP, `transaction_id` corresponde a `stripe_session_id`.

## 7) Auditoria en una sola consulta

Esta consulta consolida en una sola salida toda la traza de un `eventId`:

- sesion (`tracking_session`)
- eventos de tracking (`tracking_event`)
- orden de pago (`orders`)
- envios server-side (`integrations_log` para `GA4_MP` y `META_CAPI`)
- estado de webhooks (`stripe_webhook_event`)

Que va a desplegar:

- `section`: origen de la fila (tabla o integracion).
- `ref_1`: referencia principal (normalmente `event_id` o `reference_id`).
- `ref_2`: referencia secundaria (ej. `event_type`, `payment_intent_id`, `stripe_event_id`, HTTP status).
- `status`: estado funcional del tramo (`business_status`, estado de integracion o webhook).
- `ts`: timestamp del registro.
- `detail`: detalle adicional (errores o datos de apoyo).

El resultado sale ordenado por `ts DESC`, por lo que primero ves lo mas reciente del caso auditado.

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

Lectura rapida esperada en un flujo exitoso:

1. Filas de `tracking_session` y `tracking_event` (landing/cta/checkout).
2. Fila de `orders` con estado de negocio `SUCCESS`.
3. Filas en `ga4_mp` y/o `meta_capi` con `status` `SENT` o `SENT_WITH_WARNINGS`.
4. Filas de `stripe_webhook_event` con `status` `PROCESSED`.

## 8) Validacion del objetivo del proyecto

Estado general: `cumplido a nivel MVP funcional`.

Cumplido:

- Tracking de funnel y attribution en landing.
- Correlacion completa por `eventId` entre landing, pago y backend.
- Conteo y registro de compras con deduplicacion de webhooks/ordenes.
- Integraciones activables para Google Analytics 4 MP y Meta CAPI con auditoria.
- Dashboard admin operativo para seguimiento de sesiones, eventos y metricas.

Pendiente para cierre productivo:

- Autenticacion/autorizacion robusta en admin.
- Pruebas E2E y de carga para escenarios de concurrencia.
- Observabilidad (alertas, dashboards, SLOs y manejo de incidentes).
- Checklist operativo de despliegue y rollback.
