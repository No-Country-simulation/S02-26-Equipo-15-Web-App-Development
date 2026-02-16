# Backend API

API de tracking y pagos con Spring Boot 3 (Java 17).

## Capacidades

- `POST /api/track` con correlacion por `eventId`.
- Persistencia en PostgreSQL con Flyway.
- `POST /api/stripe/webhook` con validacion de firma Stripe.
- Idempotencia de webhooks y ordenes:
  - `stripe_webhook_event.stripe_event_id` (PK)
  - `orders.stripe_session_id` (UNIQUE)
  - `orders.payment_intent_id` (UNIQUE parcial cuando no es null)
- Integraciones server-side por feature flag:
  - Meta CAPI
  - GA4 Measurement Protocol
  - Pipedrive (opcional)
- Endpoints admin de lectura (`/api/admin/*`).
- CORS configurable.
- Manejo global de errores.
- Rate limit in-memory para `/api/track`.

## Endpoints

- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

## Contrato de track

Request:

```json
{
  "eventType": "landing_view",
  "utm_source": "...",
  "utm_medium": "...",
  "utm_campaign": "...",
  "utm_term": "...",
  "utm_content": "...",
  "gclid": "...",
  "fbclid": "...",
  "landing_path": "/",
  "eventId": "optional-uuid"
}
```

Response exacta:

```json
{ "eventId": "<uuid>" }
```

Eventos permitidos: `landing_view`, `click_cta`, `begin_checkout`, `purchase`.

## Modelo de datos activo

- `tracking_session`
- `tracking_event`
- `orders`
  - incluye `business_status` canonico (`SUCCESS|PENDING|FAILED|UNKNOWN`)
- `stripe_webhook_event`
  - incluye `event_id` para correlacion con sesion
- `integrations_log`
  - `request_payload` y `response_payload` en `jsonb`

## Migraciones Flyway

- `V1__init.sql`
- `V2__integrations_log.sql`
- `V3__normalize_integrations_log_jsonb.sql`
- `V4__drop_legacy_tables.sql`
- `V5__orders_payment_intent_unique.sql`
- `V6__stripe_webhook_event_add_event_id.sql`
- `V7__orders_add_business_status.sql`

## Variables de entorno

- `SPRING_PROFILES_ACTIVE=local|prod`
- `DATABASE_URL` o `SPRING_DATASOURCE_URL` + `SPRING_DATASOURCE_USERNAME` + `SPRING_DATASOURCE_PASSWORD`
- `STRIPE_WEBHOOK_SECRET`
- `TRACKING_ENABLED=true|false`
- `META_CAPI_ENABLED=true|false`
- `META_PIXEL_ID`
- `META_ACCESS_TOKEN`
- `GA4_MP_ENABLED=true|false`
- `GA4_MEASUREMENT_ID`
- `GA4_API_SECRET`
- `GA4_MP_DEBUG_VALIDATION_ENABLED=true|false`
- `PIPEDRIVE_ENABLED=true|false`
- `PIPEDRIVE_API_TOKEN`
- `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174`

## Ejecutar en local

```bash
cd backend
mvn -q -DskipTests package
mvn spring-boot:run
```

## Verificacion rapida

### Integraciones por eventId

```sql
SELECT integration, reference_id, status, http_status, error_message, created_at
FROM integrations_log
WHERE reference_id = 'TU_EVENT_ID'
ORDER BY created_at DESC;
```

### Confirmar no duplicados de pago

```sql
SELECT payment_intent_id, COUNT(*) c
FROM orders
WHERE payment_intent_id IS NOT NULL
GROUP BY payment_intent_id
HAVING COUNT(*) > 1;
```

```sql
SELECT stripe_session_id, COUNT(*) c
FROM orders
GROUP BY stripe_session_id
HAVING COUNT(*) > 1;
```

### Confirmar webhook correlacionado

```sql
SELECT stripe_event_id, event_id, status, error, received_at, processed_at
FROM stripe_webhook_event
ORDER BY received_at DESC
LIMIT 20;
```

### Confirmar Meta CAPI aceptado

```sql
SELECT
  status,
  http_status,
  response_payload->>'events_received' AS events_received,
  response_payload->>'fbtrace_id' AS fbtrace_id,
  created_at
FROM integrations_log
WHERE integration = 'META_CAPI'
ORDER BY created_at DESC
LIMIT 20;
```
