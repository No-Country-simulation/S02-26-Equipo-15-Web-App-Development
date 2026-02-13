# Backend API

Spring Boot 3 / Java 17 backend for tracking sessions/events, Stripe webhook processing, and admin reporting.

## Features

- `POST /api/track` with correlated `eventId` and first-touch attribution persistence.
- Flyway-managed PostgreSQL schema.
- `POST /api/stripe/webhook` with Stripe signature verification and idempotent processing.
- Feature-flagged server-side integrations:
  - Meta CAPI
  - GA4 Measurement Protocol
  - Pipedrive (optional)
- Admin read APIs:
  - `GET /api/admin/sessions`
  - `GET /api/admin/sessions/{eventId}`
  - `GET /api/admin/events`
  - `GET /api/admin/metrics`
- Configurable CORS.
- Global error handling with consistent error payload.
- Basic in-memory rate limiting for `/api/track`.

## Request/Response Contract

### Track endpoint

`POST /api/track`

Accepted request body keys:

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

Response (exact):

```json
{ "eventId": "<uuid>" }
```

## Environment Variables

- `SPRING_PROFILES_ACTIVE=local|prod`
- `DATABASE_URL` or `SPRING_DATASOURCE_URL` + `SPRING_DATASOURCE_USERNAME` + `SPRING_DATASOURCE_PASSWORD`
- `STRIPE_WEBHOOK_SECRET`
- `META_PIXEL_ID`
- `META_ACCESS_TOKEN`
- `GA4_MEASUREMENT_ID`
- `GA4_API_SECRET`
- `PIPEDRIVE_API_TOKEN`
- `TRACKING_ENABLED=true|false`
- `META_CAPI_ENABLED=true|false`
- `GA4_MP_ENABLED=true|false`
- `PIPEDRIVE_ENABLED=true|false`
- `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174`

## Build and Test

```bash
cd backend
mvn -q -DskipTests=false test
mvn -q package
```

## Health

- App health: `GET /actuator/health`
- DB health: `GET /api/health/db`
