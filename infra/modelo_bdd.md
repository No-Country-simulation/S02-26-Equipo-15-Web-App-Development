# Modelo BDD (Actualizado)

Documento alineado con el esquema real de Flyway (`V1`..`V7`) y el comportamiento actual del backend.

## 1) Modelo relacional

```mermaid
erDiagram
  TRACKING_SESSION {
    uuid event_id PK
    timestamp created_at
    timestamp last_seen_at
    varchar utm_source
    varchar utm_medium
    varchar utm_campaign
    varchar utm_term
    varchar utm_content
    varchar gclid
    varchar fbclid
    varchar landing_path
    varchar user_agent
    varchar ip_hash
  }

  TRACKING_EVENT {
    uuid id PK
    uuid event_id FK
    varchar event_type
    timestamp created_at
    varchar currency
    numeric value
    text payload_json
  }

  ORDERS {
    uuid id PK
    uuid event_id FK
    varchar stripe_session_id UK
    varchar payment_intent_id UK
    numeric amount
    varchar currency
    varchar status
    varchar business_status
    timestamp created_at
  }

  STRIPE_WEBHOOK_EVENT {
    varchar stripe_event_id PK
    uuid event_id
    timestamp received_at
    timestamp processed_at
    varchar status
    text error
  }

  INTEGRATIONS_LOG {
    uuid id PK
    varchar integration
    varchar reference_id
    varchar status
    int http_status
    int latency_ms
    jsonb request_payload
    jsonb response_payload
    text error_message
    timestamp created_at
  }

  TRACKING_SESSION ||--o{ TRACKING_EVENT : has
  TRACKING_SESSION ||--o{ ORDERS : links
```

Notas:
- `stripe_webhook_event.event_id` es correlacion logica (no FK en DB).
- `integrations_log.reference_id` es correlacion logica (normalmente `eventId` como texto).
- En DB real, `orders.payment_intent_id` es unique parcial (`WHERE payment_intent_id IS NOT NULL`).

## 2) Diccionario de tablas

### `tracking_session`

Tabla de sesion/attribution por `event_id`.

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `event_id` | `uuid` | No | PK |
| `created_at` | `timestamp` | No | primera vez vista |
| `last_seen_at` | `timestamp` | No | ultima actividad |
| `utm_source`..`utm_content` | `varchar(255)` | Si | first-touch |
| `gclid`, `fbclid` | `varchar(255)` | Si | first-touch |
| `landing_path` | `varchar(1024)` | Si | ruta de entrada |
| `user_agent` | `varchar(1024)` | Si | metadata request |
| `ip_hash` | `varchar(64)` | Si | hash de IP |

### `tracking_event`

Eventos de tracking (landing, cta, checkout y purchase).

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `id` | `uuid` | No | PK, idempotencia por `eventId|eventType` |
| `event_id` | `uuid` | No | FK a `tracking_session(event_id)` |
| `event_type` | `varchar(64)` | No | `landing_view`, `click_cta`, `begin_checkout`, `purchase` |
| `created_at` | `timestamp` | No | fecha de registro |
| `currency` | `varchar(16)` | Si | para compra |
| `value` | `numeric` | Si | para compra |
| `payload_json` | `text` | Si | payload serializado |

### `orders`

Ordenes derivadas de webhooks de Stripe.

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `id` | `uuid` | No | PK |
| `event_id` | `uuid` | Si | FK a `tracking_session(event_id)` |
| `stripe_session_id` | `varchar(255)` | No | UNIQUE |
| `payment_intent_id` | `varchar(255)` | Si | UNIQUE parcial (`IS NOT NULL`) |
| `amount` | `numeric` | No | monto |
| `currency` | `varchar(16)` | No | moneda |
| `status` | `varchar(64)` | No | estado Stripe crudo |
| `business_status` | `varchar(32)` | No | `SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN` |
| `created_at` | `timestamp` | No | fecha de alta |

### `stripe_webhook_event`

Bitacora de procesamiento de webhooks.

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `stripe_event_id` | `varchar(255)` | No | PK, idempotencia webhook |
| `event_id` | `uuid` | Si | correlacion con sesion |
| `received_at` | `timestamp` | No | recibido |
| `processed_at` | `timestamp` | Si | procesado |
| `status` | `varchar(32)` | No | `RECEIVED`, `PROCESSED`, `FAILED` |
| `error` | `text` | Si | detalle si falla |

### `integrations_log`

Auditoria de integraciones server-side (Meta CAPI y Google Analytics 4 MP).

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `id` | `uuid` | No | PK |
| `integration` | `varchar(64)` | No | `META_CAPI`, `GA4_MP` |
| `reference_id` | `varchar(255)` | Si | correlacion (`eventId` u otra clave) |
| `status` | `varchar(32)` | No | `SENT`, `FAILED`, `SKIPPED`, `SENT_WITH_WARNINGS` |
| `http_status` | `int` | Si | codigo HTTP |
| `latency_ms` | `int` | Si | latencia |
| `request_payload` | `jsonb` | Si | request persistida |
| `response_payload` | `jsonb` | Si | response persistida |
| `error_message` | `text` | Si | error resumido |
| `created_at` | `timestamp` | No | fecha de log |

## 3) Reglas de integridad e idempotencia

- `tracking_event.id` evita duplicados por evento logico (`eventId|eventType`).
- `stripe_webhook_event.stripe_event_id` evita reprocesar el mismo webhook.
- `orders.stripe_session_id` evita duplicar orden por checkout session.
- `orders.payment_intent_id` (unique parcial) evita duplicar orden por payment intent.
- `orders.business_status` normaliza `status` de Stripe para uso de negocio.

## 4) Indices activos

- `idx_tracking_event_event_id`
- `idx_tracking_event_created_at`
- `idx_tracking_event_type`
- `idx_tracking_session_created_at`
- `ux_orders_stripe_session_id`
- `ux_orders_payment_intent_id` (parcial)
- `idx_orders_event_id`
- `idx_orders_created_at`
- `idx_orders_business_status`
- `idx_stripe_webhook_event_event_id`
- `idx_integrations_log_created_at`
- `idx_integrations_log_integration`
- `idx_integrations_log_reference_id`

## 5) Evolucion del esquema (Flyway)

- `V1__init.sql`: tablas base (`tracking_session`, `tracking_event`, `orders`, `stripe_webhook_event`) e indices iniciales.
- `V2__integrations_log.sql`: crea `integrations_log`.
- `V3__normalize_integrations_log_jsonb.sql`: migra payloads de texto a `jsonb`.
- `V4__drop_legacy_tables.sql`: elimina modelo legado (`users`, `attributions`, `landing_events`, `payments`).
- `V5__orders_payment_intent_unique.sql`: deduplica historico y crea unique parcial por `payment_intent_id`.
- `V6__stripe_webhook_event_add_event_id.sql`: agrega `event_id` a webhooks para correlacion.
- `V7__orders_add_business_status.sql`: agrega y normaliza `business_status`.

## 6) SQL de verificacion operativa

```sql
-- Webhooks recientes
SELECT stripe_event_id, event_id, status, error, received_at, processed_at
FROM stripe_webhook_event
ORDER BY received_at DESC
LIMIT 20;

-- Ordenes recientes
SELECT id, event_id, stripe_session_id, payment_intent_id, status, business_status, amount, currency, created_at
FROM orders
ORDER BY created_at DESC
LIMIT 20;

-- Duplicados por payment_intent_id (debe dar 0 filas)
SELECT payment_intent_id, COUNT(*) AS c
FROM orders
WHERE payment_intent_id IS NOT NULL
GROUP BY payment_intent_id
HAVING COUNT(*) > 1;

-- Duplicados por stripe_session_id (debe dar 0 filas)
SELECT stripe_session_id, COUNT(*) AS c
FROM orders
GROUP BY stripe_session_id
HAVING COUNT(*) > 1;

-- Eventos purchase recientes
SELECT id, event_id, event_type, value, currency, created_at
FROM tracking_event
WHERE event_type = 'purchase'
ORDER BY created_at DESC
LIMIT 20;

-- Integraciones recientes
SELECT integration, reference_id, status, http_status, error_message, created_at
FROM integrations_log
ORDER BY created_at DESC
LIMIT 50;
```
