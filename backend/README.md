# Backend API

API de tracking y pagos construida con Spring Boot 3, PostgreSQL y Flyway.

## Endpoints principales

| Metodo | Endpoint | Acceso | Uso |
|---|---|---|---|
| `POST` | `/api/track` | Publico | Registra eventos de funnel y mantiene `tracking_session`/`tracking_event`. |
| `POST` | `/api/stripe/webhook` | Publico | Procesa webhooks Stripe con validacion de firma e idempotencia. |
| `GET` | `/api/admin/**` | Protegido (Basic + rol `ADMIN`) | Consultas del panel admin (`sessions`, `events`, `metrics`, `health`). |

## Auth Admin (Basic)

- Variables de entorno:
  - `ADMIN_USER`
  - `ADMIN_PASS`
- Defaults solo en `local|dev` cuando faltan variables:
  - `ADMIN_USER=admin`
  - `ADMIN_PASS=admin123`
- En `prod`, si `ADMIN_PASS` no esta seteado, la app no cae; el acceso admin queda bloqueado hasta configurar credenciales.

Ejemplos:

```bash
# sin auth -> 401
curl -i http://localhost:8080/api/admin/health

# con auth -> 200 {"ok":true}
curl -i -u "$ADMIN_USER:$ADMIN_PASS" http://localhost:8080/api/admin/health
```

## Integraciones

- Stripe webhook idempotente (`stripe_webhook_event.stripe_event_id`).
- GA4 Measurement Protocol (server-side `purchase`).
- Meta CAPI (server-side `purchase`).
- Pipedrive (feature flag).

## Pipedrive

- Objetivo: crear/actualizar lead/deal en etapas del funnel y pago.
- Estado actual: implementado por codigo y controlado por `PIPEDRIVE_ENABLED`, deshabilitado por defecto hasta configurar token y rollout operativo.
- Variables:
  - `PIPEDRIVE_ENABLED=true|false`
  - `PIPEDRIVE_API_TOKEN`
- Registro operativo: trazas de integracion en `integrations_log` cuando la integracion se ejecuta.

## Variables de entorno

- Base de datos:
  - `SPRING_DATASOURCE_URL` (requerido en server, formato JDBC)
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` (fallback)

Ejemplo Render (externo):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-render>:5432/<db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```
- Seguridad admin:
  - `ADMIN_USER`
  - `ADMIN_PASS`
- Stripe:
  - `STRIPE_WEBHOOK_SECRET`
- Tracking:
  - `TRACKING_ENABLED`
- GA4:
  - `GA4_MP_ENABLED`
  - `GA4_MEASUREMENT_ID`
  - `GA4_API_SECRET`
  - `GA4_MP_DEBUG_VALIDATION_ENABLED`
- Meta:
  - `META_CAPI_ENABLED`
  - `META_PIXEL_ID`
  - `META_ACCESS_TOKEN`
- Pipedrive:
  - `PIPEDRIVE_ENABLED`
  - `PIPEDRIVE_API_TOKEN`
- CORS:
  - `CORS_ALLOWED_ORIGINS`

## Tests

Ejecutar:

```bash
cd backend
mvn test
```

Contexto de tests:

- Perfil: `test`
- Config: `src/test/resources/application-test.yml`
- DB: H2 en memoria (`create-drop`)
- Flyway: deshabilitado en tests

Cobertura de tests principales:

- `SecurityConfigTest`: protege `/api/admin/**` y mantiene `/api/track` y `/api/stripe/webhook` publicos.
- `StripeWebhookIdempotencyTest`: evita reprocesar doble el mismo `stripeEventId` y no duplica orden/evento.
- `OrderStatusTransitionTest`: valida transicion de `orders.business_status` con eventos Stripe (`processing -> paid`).
- `TrackControllerTest`: `POST /api/track` responde `eventId` y persiste tracking.
- `TrackingServiceTest`: valida idempotencia de sesion/eventos y persistencia consistente.

## Modelo de datos y migraciones

Tablas principales:

- `tracking_session`
- `tracking_event`
- `orders` (incluye `orders.business_status`)
- `stripe_webhook_event`
- `integrations_log`

Migraciones Flyway:

- Ubicacion: `backend/src/main/resources/db/migration`
- Versiones actuales: `V1` a `V9`.

## Ejecucion local

```bash
cd backend
mvn spring-boot:run
```
