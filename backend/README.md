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
- Endpoints admin de lectura (`/api/admin/*`).
- Autenticacion HTTP Basic para `/api/admin/**` (rol `ADMIN`).
- CORS configurable.
- Manejo global de errores.
- Rate limit in-memory para `/api/track`.

## Endpoints

- `POST /api/track` (publico, sin auth)
- `POST /api/stripe/webhook` (publico, sin auth)
- `GET /api/admin/health` (protegido, requiere Basic Auth ADMIN)
- `GET /api/admin/sessions` (protegido, requiere Basic Auth ADMIN)
- `GET /api/admin/sessions/{eventId}` (protegido, requiere Basic Auth ADMIN)
- `GET /api/admin/events` (protegido, requiere Basic Auth ADMIN)
- `GET /api/admin/metrics` (protegido, requiere Basic Auth ADMIN)
- `GET /api/health/db`
- `GET /actuator/health`

## Contrato de track

Request:

```json
{
  "eventType": "landing_view",
  "landing_path": "/",
  "eventId": "optional-uuid",
  "metadata": {
    "campaignContext": "optional"
  }
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
  - contempla estados intermedios de pago (ej: `UNPAID` mapeado a `PENDING`)
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
- `V8__orders_fix_requires_payment_method_business_status.sql`
- `V9__orders_unpaid_business_status_pending.sql`

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
- `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174`
- `ADMIN_USER` (usuario admin para Basic Auth)
- `ADMIN_PASS` (password admin para Basic Auth)

## Autenticacion Admin (HTTP Basic)

- Rutas protegidas: todo `/api/admin/**`.
- Usuario/clave se toman de `ADMIN_USER` y `ADMIN_PASS`.
- Defaults solo para desarrollo local (`profile=local|dev`) cuando faltan variables:
  - `ADMIN_USER=admin`
  - `ADMIN_PASS=admin123`
- Fuera de `local|dev`, si `ADMIN_PASS` no esta definido, la app **no rompe el arranque**:
  - se crea una clave aleatoria interna en memoria y el acceso admin queda bloqueado hasta configurar `ADMIN_PASS`.
  - no se imprime `ADMIN_PASS` en logs.

### Ejemplos curl

Sin credenciales (debe dar `401`):

```bash
curl -i http://localhost:8080/api/admin/health
```

Con Basic Auth valido (debe dar `200`):

```bash
curl -i -u "$ADMIN_USER:$ADMIN_PASS" http://localhost:8080/api/admin/health
```

Endpoint publico `/api/track`:

```bash
curl -i -X POST http://localhost:8080/api/track \
  -H "Content-Type: application/json" \
  -d '{"eventType":"landing_view","landing_path":"/"}'
```

Endpoint publico `/api/stripe/webhook`:

```bash
curl -i -X POST http://localhost:8080/api/stripe/webhook \
  -H "Content-Type: application/json" \
  -d '{"id":"evt_test"}'
```

## Notas Stripe webhook

- `STRIPE_WEBHOOK_SECRET` se usa tanto en local como en Railway/produccion.
- `stripe login` se ejecuta en tu maquina local (CLI), no dentro de Railway.
- `GET /api/stripe/webhook` debe responder `405 METHOD_NOT_ALLOWED`; Stripe envia `POST`.

## Ejecutar en local

```bash
cd backend
mvn -q -DskipTests package
mvn spring-boot:run
```

## Ejecutar tests

```bash
cd backend
mvn test
```

Perfil usado en integracion:

- `test` (`src/test/resources/application-test.yml`)
- DB en memoria H2 (`create-drop`)
- Flyway deshabilitado en tests
- firma Stripe validada con secreto de test (`app.stripe.webhook-secret=test_webhook_secret`)

Cobertura clave incluida:

- `TrackControllerTest`: `POST /api/track` persiste `tracking_session` y `tracking_event`.
- `StripeWebhookIdempotencyTest`: dos `POST /api/stripe/webhook` con mismo `event_id` no duplican `orders` ni `stripe_webhook_event`.
- `OrderStatusTransitionTest`: flujo de `payment_intent.processing` a `checkout.session.completed` (`payment_status=paid`) actualiza la orden de `PENDING` a `PAID` (`business_status=SUCCESS`).

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
