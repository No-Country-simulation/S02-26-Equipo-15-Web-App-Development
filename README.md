<p align="center">
  <img src="./infra/assets/notcountry-readme-header.svg" alt="TrackSure" width="100%" />
</p>

# TrackSure - Revenue Attribution & Server-Side Conversion Tracking

## A) Resumen

TrackSure es una plataforma de atribucion de ingresos orientada a decisiones reales de negocio.
Conecta navegacion en landing, conversion en Stripe y evidencia operativa en un mismo hilo de datos.
El resultado es trazabilidad end-to-end para saber que campanas generan revenue y donde se pierde conversion.
Cada sesion, evento y pago puede auditarse de punta a punta con una clave comun: `eventId`.

## ¿Qué hace diferente a TrackSure?

- Atribucion basada en ingresos reales (Stripe), no solo clicks.
- Correlacion end-to-end con `eventId`.
- Webhook idempotente para evitar ordenes duplicadas.
- Auditoria de integraciones con evidencia de envios, latencia y estado.
- Modelo de datos preparado para analisis de funnel y operacion diaria.

## B) Problema que resuelve

El tracking tradicional pierde confiabilidad justo en el punto mas critico: la conversion.

- Los eventos browser-side pueden degradarse por ITP, bloqueadores y restricciones de cookies.
- Stripe introduce una redireccion que suele romper la correlacion de datos.
- Sin trazabilidad end-to-end, no se sabe que campana genero ingresos reales.
- Operaciones carece de evidencia tecnica para explicar discrepancias entre funnels y revenue.

## C) Como lo resuelve

TrackSure implementa un flujo orientado a consistencia operativa:

- Usa `eventId` como correlacion entre sesion, eventos, webhook y orden.
- Procesa `POST /api/stripe/webhook` con idempotencia por `stripe_event_id`.
- Persiste datos en un modelo separado por responsabilidad (`tracking_session`, `tracking_event`, `orders`).
- Normaliza el estado de negocio en `orders.business_status`.
- Registra auditoria de integraciones en `integrations_log` para GA4 MP y Meta CAPI.

## D) Arquitectura

```mermaid
flowchart LR
  U[Usuario] --> L[Landing React/Vite]
  L -->|POST /api/track| API[TrackSure API]
  L -->|Checkout| STRIPE[Stripe Checkout]
  STRIPE -->|POST /api/stripe/webhook| API

  API --> DB[(PostgreSQL en Render)]
  API --> LOGS[(integrations_log)]
  API --> ORD[(orders)]

  API --> GA4[GA4 Measurement Protocol]
  API --> META[Meta CAPI]

  D[TrackSure Dashboard en Vercel] -->|GET /api/admin/*| API
```

## E) Componentes del repositorio

- `backend/`: TrackSure API (Spring Boot 3 + Java 17).
- `frontend/landing/`: sitio de entrada y captura inicial de tracking.
- `frontend/admin/`: TrackSure Dashboard (operacion y auditoria).
- `infra/`: arquitectura, modelo de datos y guias tecnicas.

### Decision sobre `/BDD`

Se mantiene la carpeta `/BDD` como carpeta operativa para desarrollo local.
Incluye scripts SQL de apoyo (`schema.sql`, `reset.sql`) y guia de uso en DEV.
La fuente de verdad de produccion sigue siendo Flyway en `backend/src/main/resources/db/migration`.

## F) Modelo de datos y migraciones

- Motor principal: PostgreSQL (Render).
- Migraciones: Flyway (`V1` a `V9`).
- Tablas clave:
  - `tracking_session`
  - `tracking_event`
  - `orders`
  - `stripe_webhook_event`
  - `integrations_log`

Referencia completa del modelo:
- [`infra/modelo_bdd.md`](./infra/modelo_bdd.md)

## G) Endpoints y ejemplos

### Tabla de endpoints principales

| Metodo | Endpoint | Proposito | Seguridad |
|---|---|---|---|
| `POST` | `/api/track` | Registra eventos del funnel y mantiene sesion por `eventId`. | Publico + rate limit |
| `POST` | `/api/stripe/webhook` | Procesa webhook Stripe, actualiza ordenes y logs. | Publico + firma Stripe |
| `GET` | `/api/admin/health` | Verificacion de acceso admin. | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions` | Lista sesiones con filtros y paginacion. | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/sessions/{eventId}` | Trazabilidad completa de una sesion. | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/events` | Lista eventos por tipo/rango. | Basic Auth (`ADMIN`) |
| `GET` | `/api/admin/metrics` | KPIs agregados del dashboard. | Basic Auth (`ADMIN`) |
| `GET` | `/api/health/db` | Health de DB (`SELECT 1`). | Publico |
| `GET` | `/actuator/health` | Health de plataforma. | Publico |

### `POST /api/track` (ejemplo minimo)

```bash
curl -X POST "http://localhost:8080/api/track" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "landing_view",
    "utm_source": "google",
    "utm_medium": "cpc",
    "utm_campaign": "spring_sale",
    "landing_path": "/"
  }'
```

### `POST /api/stripe/webhook` (firma Stripe)

```bash
curl -X POST "http://localhost:8080/api/stripe/webhook" \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1700000000,v1=<firma_generada_por_stripe>" \
  --data-binary @payload.json
```

Nota: el header `Stripe-Signature` debe ser valido y corresponder a `STRIPE_WEBHOOK_SECRET`.

### `GET /api/admin/*` (Basic Auth)

```bash
curl -u "${ADMIN_USER}:${ADMIN_PASS}" \
  "http://localhost:8080/api/admin/health"
```

### Health checks

```bash
curl "http://localhost:8080/api/health/db"
curl "http://localhost:8080/actuator/health"
```

### Swagger

Swagger no esta habilitado en el estado actual.
Opcionalmente, puede habilitarse agregando `springdoc-openapi` al backend y exponiendo `/swagger-ui/index.html`.

## H) Variables de entorno

### Minimas para produccion (Render)

- `PORT` (inyectado por Render)
- `SPRING_DATASOURCE_URL` (JDBC valido: `jdbc:postgresql://host:port/db`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ADMIN_USER`
- `ADMIN_PASS`
- `STRIPE_WEBHOOK_SECRET`

### Opcionales (integraciones y tuning)

- `TRACKING_ENABLED`
- `GA4_MP_ENABLED`
- `GA4_MEASUREMENT_ID`
- `GA4_API_SECRET`
- `GA4_MP_DEBUG_VALIDATION_ENABLED`
- `META_CAPI_ENABLED`
- `META_PIXEL_ID`
- `META_ACCESS_TOKEN`
- `CORS_ALLOWED_ORIGINS`

### Fallback de datasource (si no se define `SPRING_DATASOURCE_URL`)

- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

## I) Como correr local

### Requisitos

- Java 17+
- Maven 3.9+
- Node.js 20+
- PostgreSQL local

### 1. Backend (TrackSure API)

```bash
cd backend
mvn spring-boot:run
```

Ejemplo rapido:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/app_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
ADMIN_USER=admin
ADMIN_PASS=admin123
```

### 2. Landing

```bash
cd frontend/landing
npm install
npm run dev
```

`.env` sugerido:

```bash
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PAYMENT_LINK=https://buy.stripe.com/...
VITE_ATTRIBUTION_DEFAULT_SOURCE=(direct)
VITE_ATTRIBUTION_DEFAULT_MEDIUM=(none)
VITE_ATTRIBUTION_DEFAULT_CAMPAIGN=(not set)
VITE_ATTRIBUTION_REFERRAL_MEDIUM=referral
VITE_ATTRIBUTION_REFERRAL_CAMPAIGN=(referral)
```

### 3. TrackSure Dashboard

```bash
cd frontend/admin
npm install
npm run dev
```

`.env` sugerido:

```bash
VITE_API_URL=http://localhost:8080
```

## J) Deploy (Render / Vercel)

### Render (TrackSure API + PostgreSQL)

- El backend escucha en `server.port=${PORT:8080}`.
- Render inyecta `PORT` automaticamente.
- Configurar datasource con:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
- Stripe debe enviar webhook a:
  - `https://<tu-servicio-render>/api/stripe/webhook`

### Vercel (Landing + TrackSure Dashboard)

- `frontend/landing` y `frontend/admin` en proyectos separados.
- En ambos, `VITE_API_URL` debe apuntar al backend en Render.

## Observabilidad y Auditoría

TrackSure registra evidencia operativa en tablas dedicadas para poder auditar cada conversion:

- `tracking_session` y `tracking_event`: contexto de navegacion y eventos del funnel.
- `orders`: consolidacion de pago y estado de negocio (`business_status`).
- `stripe_webhook_event`: recepcion, deduplicacion y resultado de procesamiento de webhooks.
- `integrations_log`: registro de envios a GA4 MP y Meta CAPI con `status`, `httpStatus` y `latencyMs`.

Con esto el equipo puede:

- demostrar si una conversion fue efectivamente procesada,
- detectar fallas de integracion o demoras de entrega,
- depurar discrepancias entre marketing, pagos y revenue,
- seguir todo el flujo con `eventId` como hilo conductor tecnico y funcional.

En TrackSure Dashboard, esta informacion se consulta por sesion y permite revisar estado de orden, webhooks e integraciones desde una sola vista.

## Estrategia de Testing

Actualmente existe cobertura focalizada en seguridad, idempotencia y consistencia de estados:

- `SecurityConfigTest`: valida proteccion de `/api/admin/**` y acceso publico en endpoints criticos.
- `StripeWebhookIdempotencyTest`: evita reprocesar el mismo evento de Stripe.
- `OrderStatusTransitionTest`: valida transiciones correctas de `business_status`.
- `TrackControllerTest` y `TrackingServiceTest`: protegen contrato y persistencia del tracking por `eventId`.

Esto es clave para evitar dos riesgos de negocio: revenue duplicado por reprocesos y perdida de trazabilidad en conversiones.

Ejecucion:

```bash
cd backend
mvn test
```

Plan de tests para ampliar cobertura:

- pruebas end-to-end del flujo `track -> webhook -> integraciones`,
- pruebas de carga para picos de webhooks,
- pruebas de resiliencia ante errores transitorios en integraciones externas.

## L) Roadmap / pendientes

- Ampliar tests de integracion end-to-end (track -> webhook -> integraciones).
- Incorporar pruebas de carga y resiliencia de webhooks.
- Habilitar Swagger de forma opcional para DX.
- Validar usabilidad del dashboard con usuarios operativos.

## Demo en 2 minutos

### A) Requisitos previos

- Landing (Vercel): https://s02-26-equipo-15-web-app-developmen.vercel.app/?utm_source=google&utm_medium=cpc&utm_campaign=demo_q1&utm_term=tax&utm_content=ad_a&gclid=g4&fbclid=meta
- Admin (Vercel): https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login
- API (Render): https://s02-26-equipo-15-web-app-development.onrender.com
- Variables minimas (si corres local): `VITE_API_URL`, `ADMIN_USER`, `ADMIN_PASS`, `STRIPE_WEBHOOK_SECRET`. Stripe test mode: tarjeta `4242 4242 4242 4242` (fecha futura, CVC cualquiera).

### B) Paso 1 - Abrir Landing (10-15s)

1. Abre la landing y valida el CTA principal `Comenzar ahora`.
2. Verifica que la propuesta de valor y bloques de servicio cargan correctamente.

![Landing de TrackSure](./infra/demo/landing-01-home.png)
_Landing productiva en Vercel con CTA inicial del flujo._

### C) Paso 2 - Generar evento (`POST /api/track`) (15-20s)

`eventId` es la llave de correlacion end-to-end: une sesion, eventos, orden, webhook y `integrations_log`.

Ejemplo Bash (`curl`) usando endpoint real en Render:

```bash
API_BASE="https://s02-26-equipo-15-web-app-development.onrender.com"

RESPONSE=$(curl -sS -X POST "$API_BASE/api/track" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "landing_view",
    "utm_source": "demo_jurado",
    "utm_medium": "manual",
    "utm_campaign": "demo_2min",
    "landing_path": "/"
  }')

echo "$RESPONSE"
EVENT_ID=$(echo "$RESPONSE" | sed -E 's/.*"eventId":"([^"]+)".*/\1/')
echo "EVENT_ID=$EVENT_ID"
```

Ejemplo PowerShell (Windows):

```powershell
$apiBase = "https://s02-26-equipo-15-web-app-development.onrender.com"
$body = @{
  eventType    = "landing_view"
  utm_source   = "demo_jurado"
  utm_medium   = "manual"
  utm_campaign = "demo_2min"
  landing_path = "/"
} | ConvertTo-Json

$response = Invoke-RestMethod -Method Post -Uri "$apiBase/api/track" -ContentType "application/json" -Body $body
$eventId = $response.eventId
$eventId
```

Guarda este `eventId`, lo usaremos para rastrear todo.

### D) Paso 3 - Completar pago Stripe (modo test) (20-30s)

1. Haz click en `Comenzar ahora` desde la landing o abre el payment link test:
   `https://buy.stripe.com/test_dRm6oH5bY54Wc84e1p4ow00`
2. Confirma que la URL de checkout incluye `client_reference_id=<eventId>`.
3. Completa el pago con tarjeta test `4242 4242 4242 4242`.

Como se propaga el `eventId` a Stripe en este proyecto:
- Frontend landing agrega `client_reference_id` y `nc_event_id` al payment link.
- Backend webhook correlaciona desde `client_reference_id` y tambien soporta `metadata.eventId|event_id|client_reference_id|tracking_event_id`.

![Stripe Checkout en modo test](./infra/demo/stripe-01-checkout-test-mode.png)
_Checkout en modo test con `client_reference_id` visible en la URL._

![Pago exitoso en Stripe](./infra/demo/stripe-02-payment-success.png)
_Confirmacion de pago exitoso antes de revisar admin y BDD._

### E) Paso 4 - Ver orden en Dashboard Admin (20-30s)

1. Login admin: https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login
2. Dashboard: valida KPIs (`Total ordenes`, `Revenue total`) y distribucion por `business_status`.
3. Sessions: busca por `eventId`, revisa estado (`SUCCESS/FAILED/PENDING/UNKNOWN`) y abre `Ver trazabilidad`.
4. Events: confirma `purchase` y su `orderId`.
5. Trace View: valida correlacion de IDs (eventId, orderId, payment_intent_id, stripe_session_id) y timeline completo.

![Login Admin](./infra/demo/admin-01-login.png)
_Ingreso al panel con credenciales configuradas en backend (`ADMIN_USER` / `ADMIN_PASS`)._

![Dashboard KPI cards](./infra/demo/admin-02-dashboard-kpis.png)
_KPIs operativos para sesiones, eventos, ordenes e ingresos._

![Dashboard business status](./infra/demo/admin-03-dashboard-business-status.png)
_Distribucion de `business_status` para lectura rapida de salud operativa._

![Sessions](./infra/demo/admin-04-sessions-list.png)
_Busqueda por `eventId` y acceso a `Ver trazabilidad`._

![Events](./infra/demo/admin-05-events-list.png)
_Eventos de tracking y vinculacion con `orderId`._

![Trace View](./infra/demo/admin-06-trace-view-overview.png)
_Vista de correlacion (session info + IDs de negocio/pago)._

![Trace Timeline](./infra/demo/admin-07-trace-view-timeline.png)
_Timeline end-to-end con eventos, webhook e integraciones._

### F) Paso 5 - Validar `integrations_log` en la BDD (15-20s)

`integrations_log` registra evidencia de envios server-side (GA4 MP y Meta CAPI).  
`status` esperado: `SENT`, `SENT_WITH_WARNINGS`, `SKIPPED` o `FAILED`.

![Integrations log correlacionada por eventId](./infra/demo/db-01-integrations-log.png)
_Validacion puntual de la integrations_log persistida y su relacion con  `reference_id`._

```sql
-- Traza completa por event_id
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

![Traza completa por eventId](./infra/demo/db-03-trace-query-result.png)
_Auditoria consolidada del flujo E2E por `eventId`._

Lectura rapida de esta captura (comportamiento esperado):
- `tracking_event` con 2 filas es normal: una para `landing_view` y otra para `purchase`.
- `stripe_webhook_event` con 2 filas tambien es normal: Stripe puede enviar mas de un evento del mismo pago (por ejemplo `payment_intent.*` y `checkout.session.completed`), cada uno con `stripe_event_id` distinto.
- Lo que no debe duplicarse es la orden: debe mantenerse 1 fila en `orders` por `payment_intent_id`/`stripe_session_id`.

![Order correlacionada por eventId](./infra/demo/db-02-order-by-event-id.png)
_Validacion puntual de la orden persistida y su relacion con `event_id`._

## URLs de referencia

| Recurso | URL |
|---|---|
| Landing | https://s02-26-equipo-15-web-app-developmen.vercel.app/?utm_source=google&utm_medium=cpc&utm_campaign=demo_q1&utm_term=tax&utm_content=ad_a&gclid=g4&fbclid=meta|
| TrackSure Dashboard | https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login |
| TrackSure API | https://s02-26-equipo-15-web-app-development.onrender.com |
