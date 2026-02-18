# Descripcion BDD (Actualizado)

## 0) Contexto funcional

Este proyecto busca mejorar la conversion de trafico pago (Google/Meta) hacia compra en Stripe para un servicio de incorporacion, impuestos y bookkeeping en EE.UU.

La BDD existe para sostener ese objetivo de negocio con:

- trazabilidad completa del funnel por `event_id`,
- registro confiable de compra sin duplicados,
- auditoria de integraciones (GA4_MP, META_CAPI, PIPEDRIVE).

## 1) Objetivo

La base de datos soporta tres necesidades:

- trazabilidad de sesiones y eventos de marketing/conversion,
- consolidacion de pagos Stripe sin duplicados,
- auditoria de integraciones server-side (Meta CAPI, GA4_MP, Pipedrive).

La clave de correlacion principal es `event_id` (UUID).

## 2) Alcance de esta documentacion

- Describe el modelo de datos activo en backend.
- Resume como se escriben los datos por endpoint.
- Define reglas de idempotencia y consistencia para auditoria.

Fuente de verdad del esquema:
- Migraciones Flyway `backend/src/main/resources/db/migration/V1..V7`.
- Script de apoyo: `BDD/pgcrypto.sql` (bootstrap del estado final).

## 3) Tablas principales

### `tracking_session`

Representa una sesion de tracking por `event_id`.

- Guarda first-touch de attribution (`utm_*`, `gclid`, `fbclid`).
- Guarda metadatos de request (`user_agent`, `ip_hash`).
- Se actualiza `last_seen_at` en cada interaccion asociada.

### `tracking_event`

Guarda eventos de comportamiento y conversion.

- `id` es deterministico por `eventId|eventType` para idempotencia.
- `event_type` esperado: `landing_view`, `click_cta`, `begin_checkout`, `purchase`.
- `payload_json` conserva el request/evento original serializado.

### `orders`

Consolida el estado de pago recibido desde webhooks Stripe.

- `stripe_session_id` es unico.
- `payment_intent_id` es unico cuando no es nulo.
- `business_status` normaliza el estado para analitica operativa:
  `SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN`.

### `stripe_webhook_event`

Bitacora de recepcion y procesamiento de webhooks.

- Evita reprocesos por `stripe_event_id` (PK).
- Incluye `event_id` para trazabilidad contra sesion/orden.
- Estados tipicos: `RECEIVED`, `PROCESSED`, `FAILED`.

### `integrations_log`

Auditoria de integraciones server-side.

- Registra integracion, referencia, resultado, latencia y payloads.
- `request_payload` y `response_payload` se almacenan en `jsonb`.
- `reference_id` suele ser `event_id` en formato texto.

Valores esperados en `integration`:

- `GA4_MP`
- `META_CAPI`
- `PIPEDRIVE`

## 4) Relaciones y correlacion

- Relacion fisica:
  - `tracking_event.event_id` -> `tracking_session.event_id` (FK).
  - `orders.event_id` -> `tracking_session.event_id` (FK).
- Relacion logica:
  - `stripe_webhook_event.event_id` correlaciona con sesion, sin FK.
  - `integrations_log.reference_id` correlaciona con `event_id`, sin FK.

## 5) Reglas clave de datos

- Idempotencia de tracking: PK de `tracking_event.id`.
- Idempotencia de webhook: PK de `stripe_webhook_event.stripe_event_id`.
- No duplicados de orden:
  - unique `orders.stripe_session_id`,
  - unique parcial `orders.payment_intent_id` cuando no es nulo.
- Normalizacion de negocio: `orders.business_status` con valor por defecto `UNKNOWN`.

## 6) Flujo de escritura por endpoint

### `POST /api/track`

- Upsert/creacion en `tracking_session`.
- Insert idempotente en `tracking_event`.
- Retorna `eventId` que se reutiliza en el funnel.

### `POST /api/stripe/webhook`

- Inserta/actualiza `stripe_webhook_event`.
- Upsert en `orders` segun `payment_intent_id` y `stripe_session_id`.
- Si el pago queda exitoso, registra `tracking_event` tipo `purchase`.
- Dispara integraciones y persiste resultado en `integrations_log`.

### `GET /api/admin/*`

- Consultas de lectura sobre `tracking_session`, `tracking_event` y `orders`.
- No generan escrituras.

## 7) Estados y semantica recomendada

- Para negocio/reporting usar `orders.business_status`.
- Para detalle tecnico conservar `orders.status` (estado Stripe original).
- En `integrations_log.status`:
  - `SENT`: enviado correctamente.
  - `SENT_WITH_WARNINGS`: aceptado con advertencias.
  - `FAILED`: fallo tecnico o respuesta no valida.
  - `SKIPPED`: integracion deshabilitada o sin configuracion.

## 8) Consultas de auditoria

```sql
-- Traza completa por event_id
WITH target AS (
  SELECT 'TU_EVENT_ID'::uuid AS event_id
)
SELECT 'session' AS section, ts.event_id::text AS ref, ts.created_at AS ts, NULL::text AS status
FROM tracking_session ts
JOIN target t ON ts.event_id = t.event_id
UNION ALL
SELECT 'event', te.event_type, te.created_at, NULL::text
FROM tracking_event te
JOIN target t ON te.event_id = t.event_id
UNION ALL
SELECT 'order', COALESCE(o.payment_intent_id, o.stripe_session_id), o.created_at, o.business_status
FROM orders o
JOIN target t ON o.event_id = t.event_id
UNION ALL
SELECT 'integration', il.integration, il.created_at, il.status
FROM integrations_log il
JOIN target t ON il.reference_id = t.event_id::text
UNION ALL
SELECT 'webhook', swe.stripe_event_id, swe.received_at, swe.status
FROM stripe_webhook_event swe
JOIN target t ON swe.event_id = t.event_id
ORDER BY ts DESC;
```

```sql
-- Verificar ausencia de duplicados de pago
SELECT payment_intent_id, COUNT(*) AS c
FROM orders
WHERE payment_intent_id IS NOT NULL
GROUP BY payment_intent_id
HAVING COUNT(*) > 1;

SELECT stripe_session_id, COUNT(*) AS c
FROM orders
GROUP BY stripe_session_id
HAVING COUNT(*) > 1;
```

## 9) Que valida esta auditoria

La traza por `event_id` deberia permitir confirmar, en orden temporal:

1. Se creo/actualizo la sesion (`tracking_session`).
2. Se registraron eventos del funnel (`tracking_event`).
3. Se consolido una orden sin duplicados (`orders`).
4. Se recibieron/procesaron webhooks (`stripe_webhook_event`).
5. Se registraron envios de integraciones (`integrations_log`).
