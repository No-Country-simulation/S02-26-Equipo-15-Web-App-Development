# TrackSure API (Backend)

API de tracking, pagos y atribucion server-side construida con Spring Boot 3, Java 17, PostgreSQL y Flyway.

## Arquitectura en capas

El backend sigue una separacion por responsabilidades:

- `controller`: expone endpoints REST (`/api/track`, `/api/stripe/webhook`, `/api/admin/*`).
- `service`: reglas de negocio (tracking, webhook Stripe, correlacion y metricas).
- `repository`: acceso a datos para `tracking_session`, `tracking_event`, `orders`, `stripe_webhook_event`, `integrations_log`.
- `integration`: conectores de salida para GA4 MP y Meta CAPI.
- `config`: seguridad, propiedades y wiring transversal.

## Endpoints

| Metodo | Endpoint | Uso | Seguridad |
|---|---|---|---|
| `GET` | `/` | Ping simple de servicio | Publico |
| `POST` | `/api/track` | Registra eventos de funnel y mantiene sesion por `eventId` | Publico + rate limit |
| `POST` | `/api/stripe/webhook` | Procesa webhook Stripe y actualiza ordenes/integraciones | Publico + firma Stripe |
| `GET` | `/api/admin/health` | Verifica credenciales admin | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions` | Lista sesiones (filtros/paginacion) | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions/{eventId}` | Devuelve trazabilidad completa de la sesion | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/events` | Lista eventos (tipo/rango/paginacion) | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/metrics` | KPIs de dashboard | Basic Auth (`ADMIN`) |
| `GET` | `/api/health/db` | Health de DB (`SELECT 1`) | Publico |
| `GET` | `/actuator/health` | Health de plataforma | Publico |

Swagger no esta habilitado en el deploy actual.

## Webhook Stripe

Pipeline principal en `StripeWebhookService`:

1. Extrae `stripe_event_id` y crea/recupera registro en `stripe_webhook_event`.
2. Valida firma con `Stripe-Signature` y `STRIPE_WEBHOOK_SECRET`.
3. Aplica idempotencia por PK (`stripe_webhook_event.stripe_event_id`).
4. Correlaciona `eventId` desde metadata/client reference.
5. Upsert de `orders` por `stripe_session_id` y/o `payment_intent_id`.
6. Actualiza `business_status` y dispara integraciones server-side cuando corresponde.

### Mapeo a `business_status`

| Estado Stripe (ejemplos) | Estado de negocio |
|---|---|
| `SUCCEEDED`, `PAID` | `SUCCESS` |
| `PROCESSING`, `REQUIRES_ACTION`, `PENDING`, `OPEN`, `UNPAID` | `PENDING` |
| `FAILED`, `CANCELED`, `REQUIRES_PAYMENT_METHOD` | `FAILED` |
| Cualquier otro | `UNKNOWN` |

## Seguridad

- Admin protegido por HTTP Basic y rol `ADMIN` (`/api/admin/**`).
- Credenciales por entorno: `ADMIN_USER` y `ADMIN_PASS`.
- Si `ADMIN_PASS` falta en `prod`, no se cae la app pero el acceso admin queda bloqueado.

## Base de datos y Flyway

- Motor objetivo: PostgreSQL (Render en produccion).
- Migraciones en: `src/main/resources/db/migration`.
- Versiones actuales: `V1` a `V9`.
- Tablas core:
  - `tracking_session`
  - `tracking_event`
  - `orders`
  - `stripe_webhook_event`
  - `integrations_log`

Estrategia:

- No se usa `ddl-auto` para evolucionar esquema en produccion.
- Todo cambio estructural va por migracion versionada.
- Las constraints unicas y PK sostienen idempotencia operativa.

## Variables de entorno (Render-ready)

### Minimas para produccion

- `SPRING_DATASOURCE_URL` (JDBC)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ADMIN_USER`
- `ADMIN_PASS`
- `STRIPE_WEBHOOK_SECRET`

### Variables funcionales

- `TRACKING_ENABLED`
- `GA4_MP_ENABLED`
- `GA4_MEASUREMENT_ID`
- `GA4_API_SECRET`
- `GA4_MP_DEBUG_VALIDATION_ENABLED`
- `META_CAPI_ENABLED`
- `META_PIXEL_ID`
- `META_ACCESS_TOKEN`
- `CORS_ALLOWED_ORIGINS`

### Fallback de conexion DB

Si no se define `SPRING_DATASOURCE_URL`, la app puede resolver con:

- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

Ejemplo Render:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-render>:5432/<db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

## Ejecucion local

```bash
cd backend
mvn spring-boot:run
```

## Testing

Ejecutar:

```bash
cd backend
mvn test
```

Cobertura actual relevante:

- `SecurityConfigTest`: valida proteccion de `/api/admin/**` y apertura de `/api/track` y `/api/stripe/webhook`.
- `StripeWebhookIdempotencyTest`: evita reprocesar el mismo `stripeEventId`.
- `OrderStatusTransitionTest`: valida transiciones de `business_status` segun eventos Stripe.
- `TrackControllerTest`: valida contrato basico de `/api/track`.
- `TrackingServiceTest`: valida persistencia e idempotencia de tracking.

## Deploy

- Servicio backend desplegado en Render.
- URL base de referencia: `https://s02-26-equipo-15-web-app-development.onrender.com`
- Webhook Stripe esperado: `https://s02-26-equipo-15-web-app-development.onrender.com/api/stripe/webhook`
