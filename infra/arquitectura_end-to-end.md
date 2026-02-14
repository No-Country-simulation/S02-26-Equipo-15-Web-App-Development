# Arquitectura End-to-End (Estado Actual)

## 1. Componentes
- **Landing**: React + Vite (Vercel)
- **Admin Dashboard**: React + Vite (Vercel)
- **Backend API**: Spring Boot 3 / Java 17 (Railway)
- **Base de datos**: PostgreSQL + Flyway (Railway)
- **Pagos**: Stripe Checkout + Webhook firmado
- **Tracking client-side**: GA4 (client), Meta Pixel (client)
- **Tracking server-side**: Meta CAPI, GA4 Measurement Protocol
- **CRM opcional**: Pipedrive

## 2. Diagrama de arquitectura
```mermaid
flowchart LR
  U[Usuario] --> L[Landing React/Vite]
  A[Admin] --> AD[Admin Dashboard React/Vite]

  L -->|POST /api/track| API[Backend API Spring Boot]
  AD -->|GET /api/admin/*| API

  L -->|Checkout| ST[Stripe Checkout]
  ST -->|POST /api/stripe/webhook| API

  L -->|Eventos client-side| GAClient[GA4 client]
  L -->|Eventos client-side| MPClient[Meta Pixel client]

  API -->|tracking_session / tracking_event / orders / stripe_webhook_event / integrations_log| DB[(PostgreSQL)]

  API -->|Purchase server-side| MCAPI[Meta CAPI]
  API -->|Purchase server-side| GA4MP[GA4 Measurement Protocol]
  API -->|Deal/Person| PD[Pipedrive]
```

## 3. Flujo transaccional real
```mermaid
sequenceDiagram
  autonumber
  participant U as Usuario
  participant L as Landing (React/Vite)
  participant API as API (Spring Boot)
  participant ST as Stripe Checkout
  participant DB as Postgres
  participant M as Meta CAPI (server)
  participant G as GA4 MP (server)
  participant P as Pipedrive (server)

  U->>L: Entra con UTM/gclid/fbclid
  L->>API: POST /api/track (eventType + attribution + landing_path + eventId opcional)
  API->>DB: Upsert tracking_session (first-touch) + insert tracking_event
  API-->>L: { "eventId": "<uuid>" }

  U->>ST: Completa pago
  ST->>API: POST /api/stripe/webhook (checkout.session.completed / payment_intent.succeeded)
  API->>API: Verifica firma Stripe
  API->>DB: Idempotencia en stripe_webhook_event + upsert order
  API->>DB: tracking_event purchase (si hay eventId)
  API->>M: Envio Purchase (flag META_CAPI_ENABLED)
  API->>G: Envio purchase (flag GA4_MP_ENABLED)
  API->>P: Envio deal/person (flag PIPEDRIVE_ENABLED)
  API->>DB: Log de resultado por integracion en integrations_log
```

## 4. Endpoints backend activos
- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

## 5. Controles operativos
- **Idempotencia track**: `tracking_event.id = UUID(nameUUID(eventId|eventType))`
- **Idempotencia webhook**: `stripe_webhook_event.stripe_event_id` (PK)
- **No duplicar orden**: `orders.stripe_session_id` (UNIQUE)
- **Rate limit**: token bucket in-memory por `ip_hash` en `/api/track`
- **Errores uniformes**: `{"error","message","details"}`
- **CORS configurable**: `CORS_ALLOWED_ORIGINS` (sin `*` en `prod`)

## 6. Estado de esquema
- El modelo legado (`users`, `attributions`, `landing_events`, `payments`) fue retirado por migracion Flyway `V4__drop_legacy_tables.sql`.
- El flujo end-to-end de esta arquitectura usa solo:
  - `tracking_session`
  - `tracking_event`
  - `orders`
  - `stripe_webhook_event`
  - `integrations_log`
