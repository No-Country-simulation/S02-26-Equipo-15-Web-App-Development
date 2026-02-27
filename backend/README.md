# TrackSure API

Backend de tracking y conversion server-side de TrackSure.
Implementa correlacion por `eventId`, procesamiento idempotente de webhooks Stripe y exposicion de datos para TrackSure Dashboard.

## Arquitectura en capas

- `controller`: contratos HTTP (`/api/track`, `/api/stripe/webhook`, `/api/admin/*`).
- `service`: reglas de negocio (tracking, orders, idempotencia, integraciones).
- `repository`: acceso a PostgreSQL.
- `integration`: conectores hacia GA4 MP y Meta CAPI.
- `config`: seguridad, CORS y propiedades de aplicacion.

## Endpoints

| Metodo | Endpoint | Uso | Seguridad |
|---|---|---|---|
| `GET` | `/` | Estado basico del servicio | Publico |
| `POST` | `/api/track` | Registra eventos del funnel y maneja sesion por `eventId` | Publico + rate limit |
| `POST` | `/api/stripe/webhook` | Procesa webhooks Stripe y actualiza `orders` | Publico + firma Stripe |
| `GET` | `/api/admin/health` | Health de autenticacion admin | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions` | Lista sesiones | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions/{eventId}` | Detalle por sesion | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/events` | Lista eventos | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/metrics` | KPIs de negocio | Basic Auth (`ADMIN`) |
| `GET` | `/api/health/db` | Health de DB | Publico |
| `GET` | `/actuator/health` | Health de plataforma | Publico |

Swagger no esta habilitado actualmente.
Opcionalmente puede agregarse con `springdoc-openapi`.

## Seguridad

- `/api/admin/**` requiere HTTP Basic con rol `ADMIN`.
- Credenciales por entorno:
  - `ADMIN_USER`
  - `ADMIN_PASS`
- En perfiles no locales sin `ADMIN_PASS`, el acceso admin queda bloqueado.

## Webhook Stripe

`StripeWebhookService` implementa:

1. Validacion de firma (`Stripe-Signature` + `STRIPE_WEBHOOK_SECRET`).
2. Idempotencia por `stripe_webhook_event.stripe_event_id`.
3. Upsert de ordenes por `stripe_session_id` / `payment_intent_id`.
4. Correlacion por `eventId` cuando esta disponible.
5. Actualizacion de `orders.business_status`.
6. Dispatch de integraciones server-side y log en `integrations_log`.

## Base de datos y Flyway

- PostgreSQL como storage principal.
- Flyway como fuente de verdad (`src/main/resources/db/migration`).
- Migraciones actuales: `V1` a `V9`.

Tablas principales:

- `tracking_session`
- `tracking_event`
- `orders`
- `stripe_webhook_event`
- `integrations_log`

## Variables de entorno

### Minimas para produccion (Render)

- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ADMIN_USER`
- `ADMIN_PASS`
- `STRIPE_WEBHOOK_SECRET`

### Opcionales

- `TRACKING_ENABLED`
- `GA4_MP_ENABLED`, `GA4_MEASUREMENT_ID`, `GA4_API_SECRET`, `GA4_MP_DEBUG_VALIDATION_ENABLED`
- `META_CAPI_ENABLED`, `META_PIXEL_ID`, `META_ACCESS_TOKEN`
- `PIPEDRIVE_ENABLED`, `PIPEDRIVE_API_TOKEN`
- `CORS_ALLOWED_ORIGINS`

Fallback de datasource:

- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Ejemplo Render valido:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-xxxx.a.oregon-postgres.render.com:5432/nocountry_rvoc?sslmode=require
SPRING_DATASOURCE_USERNAME=nocountry_rvoc_user
SPRING_DATASOURCE_PASSWORD=***
```

## Ejecucion local

```bash
cd backend
mvn spring-boot:run
```

## Testing

```bash
cd backend
mvn test
```

### Que miden los tests

- `SecurityConfigTest`
  - Verifica que `/api/admin/**` este protegido y que `/api/track` y `/api/stripe/webhook` sigan publicos.
  - Valor: evita regresiones de seguridad en despliegues.

- `StripeWebhookIdempotencyTest`
  - Verifica no reprocesar dos veces el mismo evento Stripe.
  - Valor: evita ordenes y conversiones duplicadas.

- `OrderStatusTransitionTest`
  - Verifica transiciones de `business_status`.
  - Valor: preserva consistencia de metricas de negocio.

- `TrackControllerTest`
  - Verifica contrato de entrada/salida de `/api/track`.
  - Valor: protege integracion con landing.

- `TrackingServiceTest`
  - Verifica persistencia e idempotencia del tracking.
  - Valor: mantiene trazabilidad estable por `eventId`.

## Deploy

- Plataforma: Render
- API base: `https://s02-26-equipo-15-web-app-development.onrender.com`
- Webhook Stripe: `https://s02-26-equipo-15-web-app-development.onrender.com/api/stripe/webhook`
